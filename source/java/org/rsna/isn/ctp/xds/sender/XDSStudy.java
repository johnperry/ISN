/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.*;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A database entry for a cached study.
 */
public class XDSStudy implements Serializable, Comparable<XDSStudy> {

	static final long serialVersionUID = 1L;

	static final Logger logger = Logger.getLogger(XDSStudy.class);

	String studyUID;
	File studyDir;
	int size;
	int objectsSent;
	long lastModifiedTime;
	XDSStudyStatus status;
	String destination = "";
	String destinationName = "";
	String patientID;
	String patientName;
	String studyDate;
	String modality = "";
	String bodypart = "";
	String studyDescription = "";

	/**
	 * Construct an XDSStudy.
	 * @param fo the object to be used to index the study
	 * @param studyDir the directory in which the study's objects are stored
	 */
	public XDSStudy(FileObject fo, File studyDir) {
		this.studyUID = fo.getStudyUID();
		this.studyDir = studyDir;
		this.size = 0;
		this.objectsSent = 0;
		this.lastModifiedTime = 0;
		this.status = XDSStudyStatus.OPEN;
		this.destination = null;
		this.patientID = fo.getPatientID();
		this.patientName = fo.getPatientName();
		setStudyDate(fo.getStudyDate());
		update(fo);
	}

	public synchronized void update(FileObject fo) {
		if (fo instanceof DicomObject) {
			DicomObject dob = (DicomObject)fo;
			if (modality.equals("")) modality = dob.getModality();
			if (bodypart.equals("")) bodypart = dob.getBodyPartExamined();
			if (studyDescription.equals("")) studyDescription = dob.getStudyDescription();
		}
	}

	/**
	 * Record the current time as the last modified time for this study
	 */
	public synchronized void setLastModifiedTime() {
		lastModifiedTime = System.currentTimeMillis();
	}

	/**
	 * Get the last modified time for this study
	 */
	public synchronized long getLastModifiedTime() {
		return lastModifiedTime;
	}

	/**
	 * Update the size
	 */
	public synchronized void setSize(int size) {
		this.size = size;
	}

	/**
	 * Update the number of objects sent
	 */
	public synchronized int incrementObjectsSent(int increment) {
		objectsSent += increment;
		return objectsSent;
	}

	/**
	 * Set the number of objects sent
	 */
	public synchronized int setObjectsSent(int value) {
		objectsSent = value;
		return objectsSent;
	}

	public synchronized File getDirectory() {
		return studyDir;
	}

	public synchronized List<File> getFiles() {
		File[] files = studyDir.listFiles();
		LinkedList<File> list = new LinkedList<File>();
		for (File file : files) list.add(file);
		return list;
	}

	public synchronized String getStudyUID() {
		return studyUID;
	}

	public synchronized String getPatientID() {
		return patientID;
	}

	public synchronized String getPatientName() {
		return patientName;
	}

	public synchronized String getStudyDate() {
		return studyDate;
	}

	public synchronized void setStudyDate(String date) {
		if (date == null) this.studyDate = "";
		else if (date.length() == 8) {
			this.studyDate =
				date.substring(0,4) + "."
					+ date.substring(4,6) + "."
						+ date.substring(6);
		}
		else this.studyDate = date;
	}

	public synchronized String getBodyPart() {
		return bodypart;
	}

	public synchronized String getModality() {
		return modality;
	}

	public synchronized void setModality(String modality) {
		this.modality = modality;
	}

	public synchronized void setDestination(String destination) {
		this.destination = destination;
	}

	public synchronized String getDestination() {
		return destination;
	}

	public synchronized void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	public synchronized String getDestinationName() {
		return destinationName;
	}

	public synchronized XDSStudyStatus getStatus() {
		return status;
	}

	public synchronized void setStatus(XDSStudyStatus status) {
		this.status = status;
	}

	public synchronized Document getXML() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Study");
			doc.appendChild(root);
			root.setAttribute("studyUID", studyUID);
			root.setAttribute("size", Integer.toString(size));
			root.setAttribute("objectsSent", Integer.toString(objectsSent));
			root.setAttribute("status", status.toString());
			root.setAttribute("patientID", patientID);
			root.setAttribute("patientName", patientName);
			root.setAttribute("studyDate", studyDate);
			root.setAttribute("modality", modality);
			root.setAttribute("bodypart", bodypart);
			root.setAttribute("studyDescription", studyDescription);

			String sd = studyDescription;
			if (sd.equals("")) {
				sd = modality;
				if (!sd.equals("") && !bodypart.equals("")) sd += ": "+bodypart;
				if (sd.equals("")) sd = "unavailable";
			}
			root.setAttribute("description", sd);

			root.setAttribute("destination", destination);
			root.setAttribute("destinationName", destinationName);
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to create the XML for a study");
			return null;
		}
	}

	public int compareTo(XDSStudy s) {
		return getPatientID().compareTo(s.getPatientID());
	}

}
