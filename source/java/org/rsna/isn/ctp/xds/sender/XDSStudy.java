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

	/**
	 * Construct an XDSStudy.
	 * @param studyUID the StudyInstanceUID
	 * @param studyDir the directory in which the study's objects are stored
	 * @param size the number of objects stored for the study
	 * @param objectsSent the number of objects that have been sent to the Clearinghouse
	 * @param lastModifiedTime the time the most recent object was stored in studyDir
	 * @param status the status of the study
	 * @param destination the destination key selected by the user servlet
	 * @param patientID the actual PatientID (PHI)
	 * @param patientName the actual PatientName (PHI)
	 */
	public XDSStudy(String studyUID,
					File studyDir,
					int size,
					int objectsSent,
					long lastModifiedTime,
					XDSStudyStatus status,
					String destination,
					String patientID,
					String patientName) {

		this.studyUID = studyUID;
		this.studyDir = studyDir;
		this.size = size;
		this.objectsSent = objectsSent;
		this.lastModifiedTime = lastModifiedTime;
		this.status = status;
		this.destination = destination;
		this.patientID = patientID;
		this.patientName = patientName;
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
