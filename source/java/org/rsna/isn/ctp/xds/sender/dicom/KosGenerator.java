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
package org.rsna.isn.ctp.xds.sender.dicom;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.IOUtils;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.util.UIDUtils;
import org.rsna.isn.ctp.xds.sender.event.KosEvent;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;

/**
 * Utility class for generating the KOS manifests that are part of the submission
 * set. 
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 *
 */
public class KosGenerator
{
	private KosGenerator()
	{
	}

	/**
	 * Generate the KOS object(s) for a list of studies. 
	 * 
	 * @param files A list of files to process
	 * @param listeners A list of listeners to be notified of events.
	 * @return A collection of DicomStudy objects. Each DicomStudy object 
	 * contains the KOS of the associated objects.
	 * 
	 * @throws IOException If there was an exception processing the files. 
	 */
	public static Collection<DicomStudy> processFiles(List<File> files, 
			List<XdsSubmissionListener> listeners) throws IOException
	{
		StopTagInputHandler stop = new StopTagInputHandler(Tag.PixelData);

		DicomInputStream in = null;
		try
		{
			Map<String, DicomStudy> studies = new TreeMap();

			for (int i = 0; i < files.size(); i++)
			{
				File srcFile = files.get(i);
				in = new DicomInputStream(srcFile);
				in.setHandler(stop);



				DicomObject fmi = in.readFileMetaInformation();
				if (fmi == null)
					throw new IOException(srcFile + " is not a DICOM part-10 file");

				DicomObject header = in.readDicomObject();

				String transferSyntaxUid = in.getTransferSyntax().uid();

				in.close();
				in = null;

				String studyUid = header.getString(Tag.StudyInstanceUID);
				String seriesUid = header.getString(Tag.SeriesInstanceUID);
				String sopInstanceUid = header.getString(Tag.SOPInstanceUID);
				String sopClassUid = header.getString(Tag.SOPClassUID);



				DicomStudy study = studies.get(studyUid);
				if (study == null)
				{
					study = new DicomStudy();

					study.setPatientName(header.getString(Tag.PatientName));
					study.setPatientId(header.getString(Tag.PatientID));
					study.setSex(header.getString(Tag.PatientSex));
					study.setBirthdate(header.getDate(Tag.PatientBirthDate));


					study.setAccessionNumber(header.getString(Tag.AccessionNumber));
					study.setStudyUid(studyUid);
					study.setStudyDescription(header.getString(Tag.StudyDescription));
					study.setStudyDateTime(header.getDate(Tag.StudyDate, Tag.StudyTime));
					study.setStudyId(header.getString(Tag.StudyID));
					study.setReferringPhysician(header.getString(Tag.ReferringPhysicianName));

					studies.put(studyUid, study);
				}


				DicomSeries series = study.getSeries().get(seriesUid);
				if (series == null)
				{
					series = new DicomSeries();

					series.setSeriesUid(seriesUid);
					series.setSeriesDescription(header.getString(Tag.SeriesDescription));
					series.setModality(header.getString(Tag.Modality));

					study.getSeries().put(seriesUid, series);
				}

				org.rsna.isn.ctp.xds.sender.dicom.DicomObject obj = new org.rsna.isn.ctp.xds.sender.dicom.DicomObject();

				obj.setSopClassUid(sopClassUid);
				obj.setSopInstanceUid(sopInstanceUid);
				obj.setTransferSyntaxUid(transferSyntaxUid);
				obj.setFile(srcFile);

				series.getObjects().put(sopInstanceUid, obj);

				synchronized (listeners)
				{
					KosEvent event = new KosEvent(srcFile, i + 1, files.size());
					
					for(XdsSubmissionListener listener : listeners)
					{
						listener.eventOccurred(event);
					}					
				}
			}


			for (DicomStudy study : studies.values())
				buildKos(study);



			return studies.values();
		}
		finally
		{
			IOUtils.closeQuietly(in);
		}
	}

	private static void buildKos(DicomStudy study) throws IOException
	{
		DicomObject kos = new BasicDicomObject();


		//
		// Patient module
		//
		kos.putString(Tag.PatientName, VR.PN, study.getPatientName());
		kos.putString(Tag.PatientID, VR.LO, study.getPatientId());
		kos.putString(Tag.PatientSex, VR.CS, study.getSex());
		kos.putDate(Tag.PatientBirthDate, VR.DA, study.getBirthdate());


		//
		// General study module
		//
		kos.putString(Tag.StudyInstanceUID, VR.UI, study.getStudyUid());
		kos.putString(Tag.AccessionNumber, VR.SH, study.getAccessionNumber());
		kos.putString(Tag.StudyDescription, VR.LO, study.getStudyDescription());
		kos.putDate(Tag.StudyDate, VR.DA, study.getStudyDateTime());
		kos.putDate(Tag.StudyTime, VR.TM, study.getStudyDateTime());
		kos.putString(Tag.StudyID, VR.SH, study.getStudyId());
		kos.putString(Tag.ReferringPhysicianName, VR.PN, study.getReferringPhysician());


		//
		// Key object document series module
		//
		kos.putString(Tag.Modality, VR.CS, "KO");
		kos.putString(Tag.SeriesInstanceUID, VR.UI, UIDUtils.createUID());
		kos.putString(Tag.SeriesNumber, VR.IS, "1");
		kos.putNull(Tag.ReferencedPerformedProcedureStepSequence, VR.SQ);


		//
		// General equipment module
		//
		kos.putString(Tag.Manufacturer, VR.LO, "RSNA");


		//
		// Key object document module
		//
		kos.putString(Tag.InstanceNumber, VR.IS, "1");
		Date now = new Date();
		kos.putDate(Tag.ContentDate, VR.DA, now);
		kos.putDate(Tag.ContentTime, VR.TM, now);

		DicomObject evidenceSeq = new BasicDicomObject();
		evidenceSeq.putString(Tag.StudyInstanceUID, VR.UI, study.getStudyUid());

		DicomElement refSeriesSeq = evidenceSeq.putSequence(Tag.ReferencedSeriesSequence);
		for (DicomSeries series : study.getSeries().values())
		{
			DicomObject seriesItem = new BasicDicomObject();
			seriesItem.putString(Tag.SeriesInstanceUID, VR.UI, series.getSeriesUid());

			DicomElement refSopSeq = seriesItem.putSequence(Tag.ReferencedSOPSequence);
			for (org.rsna.isn.ctp.xds.sender.dicom.DicomObject obj : series.getObjects().values())
			{
				DicomObject objItem = new BasicDicomObject();

				objItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, obj.getSopInstanceUid());
				objItem.putString(Tag.ReferencedSOPClassUID, VR.UI, obj.getSopClassUid());

				refSopSeq.addDicomObject(objItem);
			}

			refSeriesSeq.addDicomObject(seriesItem);
		}

		kos.putNestedDicomObject(Tag.CurrentRequestedProcedureEvidenceSequence, evidenceSeq);



		//
		// SR document content module
		//

		kos.putString(Tag.ValueType, VR.CS, "CONTAINER");

		DicomElement codeSeq = kos.putSequence(Tag.ConceptNameCodeSequence);
		DicomObject codeItem = new BasicDicomObject();
		codeItem.putString(Tag.CodeValue, VR.SH, "113030");
		codeItem.putString(Tag.CodingSchemeDesignator, VR.SH, "DCM");
		codeItem.putString(Tag.CodeMeaning, VR.LO, "Manifest");
		codeSeq.addDicomObject(codeItem);

		kos.putString(Tag.ContinuityOfContent, VR.CS, "SEPARATE");


		DicomElement contentSeq = kos.putSequence(Tag.ContentSequence);
		for (DicomSeries series : study.getSeries().values())
		{
			for (org.rsna.isn.ctp.xds.sender.dicom.DicomObject obj : series.getObjects().values())
			{
				DicomObject contentItem = new BasicDicomObject();
				contentItem.putString(Tag.ValueType, VR.CS, "IMAGE");
				contentItem.putString(Tag.RelationshipType, VR.CS, "CONTAINS");

				DicomElement contentSopSeq = contentItem.putSequence(Tag.ReferencedSOPSequence);
				DicomObject contentSopItem = new BasicDicomObject();
				contentSopItem.putString(Tag.ReferencedSOPClassUID, VR.UI, obj.getSopClassUid());
				contentSopItem.putString(Tag.ReferencedSOPInstanceUID, VR.UI, obj.getSopInstanceUid());
				contentSopSeq.addDicomObject(contentSopItem);

				contentSeq.addDicomObject(contentItem);
			}
		}




		//
		// SOP common module
		//
		String kosSopInstanceUid = UIDUtils.createUID();
		kos.putString(Tag.SOPInstanceUID, VR.UI, kosSopInstanceUid);

		String kosSopClassUid = UID.KeyObjectSelectionDocumentStorage;
		kos.putString(Tag.SOPClassUID, VR.UI, kosSopClassUid);

		study.setKos(kos);
	}

}
