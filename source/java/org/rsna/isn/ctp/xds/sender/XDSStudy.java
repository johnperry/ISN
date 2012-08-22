/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.io.Serializable;
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
	String destination;
	String patientID;
	String patientName;
	String studyDate;
	String modality;

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
		if (fo instanceof DicomObject) this.modality = ((DicomObject)fo).getModality();
		else this.modality = "";
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

	public File getDirectory() {
		return studyDir;
	}

	public String getStudyUID() {
		return studyUID;
	}

	public String getPatientID() {
		return patientID;
	}

	public String getPatientName() {
		return patientName;
	}

	public String getStudyDate() {
		return studyDate;
	}

	public void setStudyDate(String date) {
		if (date == null) this.studyDate = "";
		else if (date.length() == 8) {
			this.studyDate =
				date.substring(0,4) + "."
					+ date.substring(4,6) + "."
						+ date.substring(6);
		}
		else this.studyDate = date;
	}

	public String getModality() {
		return modality;
	}

	public void setModality(String modality) {
		this.modality = modality;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getDestination() {
		return destination;
	}

	public XDSStudyStatus getStatus() {
		return status;
	}

	public void setStatus(XDSStudyStatus status) {
		this.status = status;
	}

	public Document getXML() {
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
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to create the XML for a study");
			return null;
		}
	}

	public int compareTo(XDSStudy s) {
		return patientID.compareTo(s.getPatientID());
	}

}
