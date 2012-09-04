/* Copyright (c) <2011>, <Radiological Society of North America>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the <RSNA> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package org.rsna.isn.ctp.xds.receiver;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import javax.activation.DataSource;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.util.StringUtils;
import org.dcm4che2.util.UIDUtils;
import org.rsna.isn.ctp.xds.rad69.RegistryErrorList;
import org.rsna.isn.ctp.xds.rad69.RetrieveDocumentSetRequestType.DocumentRequest;
import org.rsna.isn.ctp.xds.rad69.RetrieveDocumentSetResponseType;
import org.rsna.isn.ctp.xds.rad69.RetrieveDocumentSetResponseType.DocumentResponse;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType.StudyRequest;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType.TransferSyntaxUIDList;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest;
import org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType.TransferSyntaxUIDList;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Retrieve image.
 *
 * @version @author
 * 1.0.0    oyesanyf
 * 1.0.0    Wendy Zhu
 *
 */
public class RetrieveDocuments {

    static final Logger logger = Logger.getLogger(RetrieveDocuments.class);

    Map<String, String[]> pcs = new TreeMap();
    File queue = null;
    File tmp = null;
    DocSetDB docsetDB = null;
    String siteID = null;

    /**
        * Construct a RetrieveDocuments.
        * @param tmp the temp folder to store the retrieved objects,
        * @param docsetDB the db object to keep index of retrieved submissionSetID
        * @param siteID string of hashed rsnaID
        */
    public RetrieveDocuments(File tmp, DocSetDB docsetDB, String siteID) throws Exception {
        this.tmp = tmp;
        this.docsetDB = docsetDB;
        this.siteID = siteID;
        getPresentationContext();
    }

    /**
        * Retrieve SubmissionSets and documents under it including KOS and report
        * @return a list of DocumentInfo each element disc one study, or empty list if no file is available
        */
    public List<DocumentInfo> getSubmissionSets() {
        List<DocumentInfo> docInfoList = new LinkedList<DocumentInfo>();
        DocumentInfo docInfo = null;
        List<File> reportFiles = new LinkedList<File>();
        String studyUID = "";

        //Get SubmissionSetIDs and DocumentIDs under each SubmissionSet
        ITI18 query18 = new ITI18();
        ITI18DataType input18 = new ITI18DataType();

        try {
            input18.setRegistryURL(XDSConfiguration.getInstance().registryURL);
            input18.setRepositoryUniqueID(XDSConfiguration.getInstance().repositoryUniqueID);
            input18.setAssigningAuthorityUniversalId(XDSConfiguration.getInstance().assigningAuthorityUniversalID);
            input18.setAssigningAuthorityUniversalIdType(XDSConfiguration.getInstance().assigningAuthorityUniversalIDType);
            input18.setPatientID(siteID);

            HashMap<String,ArrayList<String>> docList = query18.queryDocuments(input18);
            if (!docList.isEmpty()) {
                //Retrieve documents by DocumentUniqueIDs (KOS & Report)
                ITI43 query43 = new ITI43();
                ITI43DataType input43 = new ITI43DataType();

                input43.setRepositoryURL(XDSConfiguration.getInstance().repositoryURL);
                input43.setRepositoryUniqueId(XDSConfiguration.getInstance().repositoryUniqueID);
                input43.setHomeCommunityId(XDSConfiguration.getInstance().homeCommunityID);
                input43.setDownloadDIR(tmp.getAbsolutePath().toString());

                //Loop through all submissionSetIDs under this siteID
                Iterator ssItr = docList.keySet().iterator();
                while (ssItr.hasNext()) {
                    String submissionSetID = (String) ssItr.next();
                    //if the submissionID has been retrieved, then go to next one
                    if (!docsetDB.contains(submissionSetID)) {
                        logger.info("Get documents for submissionSetID " + submissionSetID);

                        //get all documents under this submissionSetID and store in tmp folder
                        Iterator<String> docItr = docList.get(submissionSetID).iterator();
                        while (docItr.hasNext()) {
                            String docID = docItr.next();

                            input43.setDocumentUniqueId(docID);
                            File downloadedFile = query43.queryDocuments(input43, siteID);

                            //if a file was retrieved, check if KOS then fill in docInfo object
                            //else add to reportFiles list
                            if (downloadedFile.getAbsolutePath().toString() != null) {
                                try {
                                    docInfo = processFile(downloadedFile, docID);
                                    if (docInfo == null) {
                                        reportFiles.add(downloadedFile);
                                    }
                                    else {
                                        studyUID = docInfo.getStudyInstanceUID();
                                        docInfoList.add(docInfo);
                                    }
                                } catch (Exception e) {
                                    logger.error(e.getMessage());
                                }
                            }
                        }

                        docsetDB.addID(submissionSetID);

                        //Convert report files to XML and assoiciate with studyUID
                        if (studyUID.length() > 0) {
                            for(File f : reportFiles){
                                convertToXML(f.getName(), studyUID, f);
                            }
                            studyUID = "";
                            docInfo = null;
                            reportFiles.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("getSubmissionSets Error for [" + siteID + "]:" +e.getMessage(),e);
        }

        return docInfoList;
    }

    /**
    * Retrieve images for a given studay
    * @param docInfo study meta data
    * @return number of images were retrieved
    */
    public int getStudy (DocumentInfo docInfo) {
        int imageCounter = 0;
        int imagesOfStudy = 0;

        DataHandler dh;
        FileOutputStream fos;
        String studyInstanceUID;
        String seriesInstanceUID;
        String transferSyntaxUID;
        String documentUniqueId;
        String status;

        RetrieveImagingDocumentSetRequestType dsRequest;
        StudyRequest studyRequest;
        List<StudyRequest> studyRequestList;
        SeriesRequest seriesRequest;
        List<SeriesRequest> seriesRequestList;
        DocumentRequest docRequest;
        List<DocumentRequest> documentRequestList;
        ArrayList<String> sopInstanceUIDList;

        RetrieveDocumentSetResponseType dsResponse;
        List<DocumentResponse> responseList;
        DocumentResponse documentResponse;
        RegistryErrorList errList;

        try {
            studyInstanceUID = docInfo.getStudyInstanceUID();
            transferSyntaxUID = docInfo.getTransferSyntaxUID();

            TransferSyntaxUIDList transferList = new TransferSyntaxUIDList();
            List<String> transferSyntaxUIDs = transferList.getTransferSyntaxUID();
            transferSyntaxUIDs.add(transferSyntaxUID);

            //-- Enable transfer syntax support
            String sopClassUids[] = pcs.keySet().toArray(new String[0]);
            for (String sopClassUid : sopClassUids)
            {
                String txUids[] = pcs.get(sopClassUid);
                for (String txUid : txUids)
                {
                    if (txUid.length() > 0)
                    {
                        transferSyntaxUIDs.add(txUid);
                    }
                }
            }

            ArrayList<String> seriesInstanceUIDList = docInfo.getSeriesInstanceUIDList();
            if (seriesInstanceUIDList != null) {
                //Generate Rad69 request meta data dsRequest per Series
                Iterator seriesItr = seriesInstanceUIDList.iterator();
                while (seriesItr.hasNext()){
                    seriesInstanceUID = (String) seriesItr.next();

                    dsRequest = new RetrieveImagingDocumentSetRequestType();
                    dsRequest.setTransferSyntaxUIDList(transferList);

                    studyRequest = new StudyRequest();
                    studyRequest.setStudyInstanceUID(studyInstanceUID);
                    studyRequestList = dsRequest.getStudyRequest();
                    studyRequestList.add(studyRequest);

                    seriesRequest = new SeriesRequest();
                    seriesRequest.setSeriesInstanceUID(seriesInstanceUID);
                    seriesRequestList = studyRequest.getSeriesRequest();
                    seriesRequestList.add(seriesRequest);

                    documentRequestList = seriesRequest.getDocumentRequest();
                    sopInstanceUIDList = (ArrayList<String>) docInfo.getImages().get(seriesInstanceUID);
                    if (sopInstanceUIDList != null) {
                        int imagesBySeries = sopInstanceUIDList.size();
                        imagesOfStudy += imagesBySeries;
                        int totalCounter = 0;
                        int requestCounter = 0;
                        Iterator imagesItr = sopInstanceUIDList.iterator();
                        /*
                         * Send request with X number of images each time
                         */
                        while (imagesItr.hasNext()){
                            documentUniqueId = (String) imagesItr.next();
                            totalCounter = totalCounter + 1;
                            requestCounter = requestCounter + 1;

                            /*
                             * Add image into dsRequest/studyRequestList/seriesRequestList/documentRequestList
                             * Send the request when accumulate X number of images
                             * After send, clear the documentRequestList
                             */
                            docRequest = new DocumentRequest();
                            docRequest.setDocumentUniqueId(documentUniqueId);
                            docRequest.setHomeCommunityId(XDSConfiguration.getInstance().homeCommunityID);
                            docRequest.setRepositoryUniqueId(XDSConfiguration.getInstance().repositoryUniqueID);
                            documentRequestList.add(docRequest);

                            if (requestCounter >= XDSConfiguration.getInstance().imagesPerRequest || totalCounter >= imagesBySeries)
                            {
                               /*
                                * Generate Rad69 response meta data dsResponse
                                * Send the request and save the retrieved images
                                */
                                //dsResponse = new RetrieveDocumentSetResponseType();
                                dsResponse = imagingDocumentSourceRetrieveImagingDocumentSet(dsRequest);
                                responseList = dsResponse.getDocumentResponse();

                                if (responseList.isEmpty()) {
                                    status = dsResponse.getRegistryResponse().getStatus();
                                    System.out.println("NO DOCUMENTS FOUND " + status);
                                    logger.info("Registry response for this query is " + status);
                                }
                                else
                                {
                                    int returnedDocs = responseList.size();
                                    imageCounter += returnedDocs;

                                    documentResponse = new DocumentResponse();
                                    for (int i = 0; i < returnedDocs; i++) {
                                        documentResponse = responseList.get(i);
                                        byte[] document = documentResponse.getDocument();

                                        DataSource dataSource = new ByteArrayDataSource(document, "application/octet-stream");
                                        dh = new DataHandler(dataSource);

                                        String filename = documentResponse.getDocumentUniqueId();
                                        File dcmFile = null;
                                        try {
                                            dcmFile = new File(tmp, filename + ".dcm");
											fos = new FileOutputStream(dcmFile);
											dh.writeTo(fos);
											fos.close();
											logger.info("saved "+dcmFile);
                                       } catch (Exception e) {
                                            logger.error("Error saving file for studyUID#" + studyInstanceUID, e);
                                       }
                                    }

                                    /*
                                     * Clean up the request counter and image request list
                                     */
                                    requestCounter = 0;
                                    documentRequestList.clear();
                                }
                            }
                        }
					}
				}

            }
            //RegistryResponseType registryResponse = new RegistryResponseType();
        } catch (Exception e) {
            logger.error("getStudy Error for["+docInfo.getStudyInstanceUID()+"]:"+e.getMessage(),e);
        }
        return imagesOfStudy;
    }

    /**
    * Move all retrieved files from tmp to queue.
    */
    public void moveFiles() {
        try {
        for (File fs : tmp.listFiles()){
            File fd = new File(queue, fs.getName());
            fs.renameTo(fd);
        }
        } catch (Exception e) {
            logger.error("Unable to move file to queue:" + e.getMessage());
        }
    }

    private RetrieveDocumentSetResponseType imagingDocumentSourceRetrieveImagingDocumentSet(org.rsna.isn.ctp.xds.rad69.RetrieveImagingDocumentSetRequestType body) {
        RetrieveDocumentSetResponseType imagingDocumentSet = null;

        try {
			org.rsna.isn.ctp.xds.rad69.ImagingDocumentSourceService service = new org.rsna.isn.ctp.xds.rad69.ImagingDocumentSourceService();

            MTOMFeature feature = new MTOMFeature();

        	Timer t = new Timer();
			org.rsna.isn.ctp.xds.rad69.ImagingDocumentSourcePortType port = service.getImagingDocumentSourcePortSoap12(feature);
        	logger.info("getImagingDocumentSourcePortSoap12: " + t.getElapsedTime());

            Map<String, Object> ctxt = ((BindingProvider) port).getRequestContext();
            ctxt.put("com.sun.xml.internal.ws.transport.http.client.streaming.chunk.size", 1048576);

			t.reset();
			imagingDocumentSet = port.imagingDocumentSourceRetrieveImagingDocumentSet(body);
        	logger.info("imagingDocumentSourceRetrieveImagingDocumentSet: " + t.getElapsedTime());

        } catch (Exception e) {
             logger.error("Error for imagingDocumentSourceRetrieveImagingDocumentSet : ",e);
        }

        return imagingDocumentSet;
    }

    /**
        * Check if KOS file, if so read file.
        * @return DocumentInfo object, or null if not kos file
        */
    private DocumentInfo processFile(File inFile, String documentUniqueID) {
        DocumentInfo docInfo = null;
        String mimeType;

        //decide KOS or Report file
        DicomObject object = null;
        DicomInputStream dis = null;
        try {
            dis = new DicomInputStream(inFile);
            object = dis.readDicomObject();
            mimeType = "application/dicom-kos";
        } catch (Exception e) {
            mimeType = "text/plain";
        }

        try {
            //Read KOS into docInfo
            if (mimeType.equals("application/dicom-kos")) {
                dis.close();
                ReadKOS readKOS = new ReadKOS();
                docInfo = readKOS.listHeader(object, this.siteID, documentUniqueID);
            }
        } catch (Exception e) {}
        return docInfo;
    }

    /**
        * Convert a text report file to XML.
        * @param uid the object UID
        * @param studyUID the StudyInstanceUID of the study to which the report belongs
        * @return the file pointing to the converted report
        */
    private File convertToXML(String uid, String studyUID, File file) {
        try {
                String text = FileUtil.getText(file);
                Document doc = XmlUtil.getDocument();
                Element root = doc.createElement("Report");
                doc.appendChild(root);
                root.setAttribute("uid", uid);
                root.setAttribute("StudyInstanceUID", studyUID);
                CDATASection cdata = doc.createCDATASection(text);
                root.appendChild(cdata);
                FileUtil.setText(file, XmlUtil.toString(doc));
        }
        catch (Exception returnUnmodifiedFile) { }
        return file;
    }

    /**
     * Load supported Presentation Context/ Transfer Syntax
     */
    private void getPresentationContext()
    {
		FileInputStream in = null;
        try {
            Properties props = new Properties();
            String name = "xds-dicom.properties";
            File propFile = new File(XDSConfiguration.getInstance().rootDir, name);
            FileUtil.getFile(propFile, "/"+name);
            if (propFile.exists())
            {
                in = new FileInputStream(propFile);
                props.load(in);
            }

            // Setup the presentation contexts
            for (String sopClass : props.stringPropertyNames())
            {
                if (UIDUtils.isValidUID(sopClass))
                {
                    String value = props.getProperty(sopClass);
                    String txUids[] = StringUtils.split(value, ',');
                    for (int i=0; i<txUids.length; i++)
                    {
                        String txUid = txUids[i].trim();
                        if (UIDUtils.isValidUID(txUid))
                        {
                            txUids[i] = txUid;
                        }
                        else
                        {
                            txUids[i] = "";
                        }
                    }
                    pcs.put(sopClass, txUids);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading transfer syntaxes: " + e.getMessage());
        }
        finally { FileUtil.close(in); }
    }
}
