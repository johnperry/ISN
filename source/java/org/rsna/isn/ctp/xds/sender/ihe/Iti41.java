/* Copyright (c) <2010>, <Radiological Society of North America>
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
package org.rsna.isn.ctp.xds.sender.ihe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import org.apache.axis2.client.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dcm4che2.data.UID;
import org.dcm4che2.util.UIDUtils;
import org.openhealthtools.ihe.common.ws.IHESOAP12Sender;
import org.openhealthtools.ihe.xds.document.DocumentDescriptor;
import org.openhealthtools.ihe.xds.document.XDSDocument;
import org.openhealthtools.ihe.xds.document.XDSDocumentFromByteArray;
import org.openhealthtools.ihe.xds.metadata.CodedMetadataType;
import org.openhealthtools.ihe.xds.metadata.DocumentEntryType;
import org.openhealthtools.ihe.xds.metadata.MetadataFactory;
import org.openhealthtools.ihe.xds.metadata.SubmissionSetType;
import org.openhealthtools.ihe.xds.metadata.transform.ByteArrayProvideAndRegisterDocumentSetTransformer;
import org.openhealthtools.ihe.xds.response.XDSErrorListType;
import org.openhealthtools.ihe.xds.response.XDSErrorType;
import org.openhealthtools.ihe.xds.response.XDSResponseType;
import org.openhealthtools.ihe.xds.response.XDSStatusType;
import org.openhealthtools.ihe.xds.source.B_Source;
import org.openhealthtools.ihe.xds.source.SubmitTransactionData;
import org.rsna.isn.ctp.xds.sender.dicom.DicomObject;
import org.rsna.isn.ctp.xds.sender.dicom.DicomSeries;
import org.rsna.isn.ctp.xds.sender.dicom.DicomStudy;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;

import org.rsna.util.FileUtil;

/**
 * This class implements the ITI-41 (Submit and register document set)
 * transaction.
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 *
 */
public class Iti41
{
	private static final Logger logger = Logger.getLogger(Iti41.class);

	private static final MetadataFactory xdsFactory = MetadataFactory.eINSTANCE;

	private static final String DICOM_UID_REG_UID = "1.2.840.10008.2.6.1";

	private static final DocumentDescriptor KOS_DESCRIPTOR =
			new DocumentDescriptor("KOS", "application/dicom-kos");

	private Iti41()
	{
	}

	/**
	 * Perform the actual submission to the document repository. Note: to obtain
	 * an XML dump of the XDS submission set metadata, set the system property
	 * &quot;xds-dump-dir&quot; to a directory path.
	 *
	 *
	 * @param study The study to be submitted.
	 * @param hash The hash to assign to the submission.
	 * @param endpoint The URL of the remote endpoint for the ITI-41 transaction.
	 * @param sourceId The source id to be used in the ITI-41 transaction.
	 * @param timeout The timeout (in milliseconds) to be used for the ITI-41
	 * transaction.
	 * @param listeners A list of listeners to be notified of events during the
	 * ITI-41 transaction.
	 * @param currentIndex The current zero based index of the first file within
	 * the study.  This parameter is used in the Iti41Event for notifying listeners
	 * of submission progress.
	 * @param total The total number of files that are going to be submitted.
	 * This parameter is used in the Iti41Event for notifying listeners
	 * of submission progress.
	 * @return The total number of files submitted in this transaction.
	 * @throws Exception If there was an error processing the submission set.
	 */
	public static int submitDocuments(DicomStudy study, String hash,
			URI endpoint, String sourceId, long timeout,
			List<XdsSubmissionListener> listeners, int currentIndex, int total) throws Exception
	{
		SubmitTransactionData tx = new SubmitTransactionData();
		XdsDocumentInitializer initializer = new XdsDocumentInitializer(study, hash);

		// Add entry for KOS
		XDSDocument kosDoc =
				new XDSDocumentFromByteArray(KOS_DESCRIPTOR, study.getKos());
		String kosUuid = tx.addDocument(kosDoc);
		DocumentEntryType kosEntry = tx.getDocumentEntry(kosUuid);
		initializer.initDocEntry(kosEntry);

		CodedMetadataType kosFmt = xdsFactory.createCodedMetadataType();
		kosFmt.setCode(UID.KeyObjectSelectionDocumentStorage);
		kosFmt.setDisplayName(XdsUtil.toInternationalString(UID.KeyObjectSelectionDocumentStorage));
		kosFmt.setSchemeName(DICOM_UID_REG_UID);
		kosFmt.setSchemeUUID(DICOM_UID_REG_UID);
		kosEntry.setFormatCode(kosFmt);

		kosEntry.setMimeType(KOS_DESCRIPTOR.getMimeType());

		kosEntry.setUniqueId(study.getKosSopInstanceUid());

		// Add entries for images
		for (DicomSeries series : study.getSeries().values())
		{
			for (DicomObject object : series.getObjects().values())
			{
				File dcmFile = object.getFile();

				XDSDocument dcmDoc = new LazyLoadedXdsDocument(DocumentDescriptor.DICOM,
						dcmFile, listeners, currentIndex++, total);

				String dcmUuid = tx.addDocument(dcmDoc);
				DocumentEntryType dcmEntry = tx.getDocumentEntry(dcmUuid);
				initializer.initDocEntry(dcmEntry);

				CodedMetadataType dcmFmt = xdsFactory.createCodedMetadataType();
				String sopClass = object.getSopClassUid();
				dcmFmt.setCode(sopClass);
				dcmFmt.setDisplayName(XdsUtil.toInternationalString(sopClass));
				dcmFmt.setSchemeName(DICOM_UID_REG_UID);
				dcmFmt.setSchemeUUID(DICOM_UID_REG_UID);
				dcmEntry.setFormatCode(dcmFmt);

				dcmEntry.setMimeType(DocumentDescriptor.DICOM.getMimeType());

				dcmEntry.setUniqueId(object.getSopInstanceUid());
			}
		}

		// Initialize submission set metadata
		SubmissionSetType subSet = tx.getSubmissionSet();

		subSet.setAuthor(initializer.getAuthor());

		CodedMetadataType contentType = xdsFactory.createCodedMetadataType();
		contentType.setCode("Imaging Exam");
		contentType.setDisplayName(XdsUtil.toInternationalString("Imaging Exam"));
		contentType.setSchemeName("RSNA-ISN");
		subSet.setContentTypeCode(contentType);

		subSet.setPatientId(initializer.getHash());
		subSet.setSourceId(sourceId);
		subSet.setSubmissionTime(XdsUtil.toGmtString(new Date()));
		subSet.setTitle(XdsUtil.toInternationalString(study.getStudyDescription()));
		subSet.setUniqueId(UIDUtils.createUID());

		ByteArrayProvideAndRegisterDocumentSetTransformer setTransformer =
				new ByteArrayProvideAndRegisterDocumentSetTransformer();
		setTransformer.transform(tx.getMetadata());

		String debugDirName = System.getProperty("xds-dump-dir");
		if (StringUtils.isNotBlank(debugDirName))
		{
			File debugDir = new File(debugDirName);
			debugDir.mkdirs();

			if (debugDir.isDirectory())
			{
				FileOutputStream fos = null;
				try
				{
					File debugFile = new File(debugDir, study.getStudyUid() + ".xml");

					fos = new FileOutputStream(debugFile);
					fos.write(setTransformer.getMetadataByteArray());

					logger.info("Wrote dump of XDS submission to: " + debugFile);
				}
				finally
				{
					IOUtils.closeQuietly(fos);
				}
			}
		}

		B_Source reg = new B_Source(endpoint);
		IHESOAP12Sender sender = (IHESOAP12Sender) reg.getSenderClient().getSender();

		Options options = sender.getAxisServiceClient().getOptions();
		options.setTimeOutInMilliSeconds(timeout);

		XDSResponseType resp = reg.submit(tx);

		XDSStatusType status = resp.getStatus();
		int code = status.getValue();

		if (code != XDSStatusType.SUCCESS)
		{
			XDSErrorListType errors = resp.getErrorList();
			List<XDSErrorType> errorList = errors.getError();

			String chMsg = status.getLiteral();
			for (XDSErrorType error : errorList)
			{
				chMsg = error.getCodeContext();
				chMsg = StringUtils.removeStart(chMsg,
						"com.axonmed.xds.registry.exceptions.RegistryException: ");
				break;
			}
			throw new ClearinghouseException("Submission of study "
					+ study.getStudyUid()
					+ " failed. Clearinghouse returned error: " + chMsg);

		}

		return currentIndex;
	}

}
