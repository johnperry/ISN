/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.*;
import org.rsna.ctp.pipeline.Status;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.rsna.isn.ctp.xds.sender.event.*;

/**
 * A cache for studies.
 */
public class XDSStudyCache {

	static final Logger logger = Logger.getLogger(XDSStudyCache.class);

	private static Hashtable<String,XDSStudyCache> caches = new Hashtable<String,XDSStudyCache>();
	private Element element;
	private File cacheRoot;
	private File indexRoot;
	private String context;
	private XDSDatabase database;
	private ExecutorService execSvc;

	final int maxThreads = 4;

	/**
	 * Construct an XDSStudyCache.
	 * @param context
	 * @param root the root directory of the cache
	 */
	protected XDSStudyCache(String context, File root, Element element) {
		this.element = element;
		this.context = context;
		this.cacheRoot = new File(root, "cache");
		cacheRoot.mkdirs();
		this.indexRoot = new File(root, "index");
		indexRoot.mkdirs();
		this.database = new XDSDatabase(indexRoot);
		this.execSvc = Executors.newFixedThreadPool( maxThreads );

	}

	/**
	 * Get the current singleton instance of the XDSStudyCache
	 * for a specified context. creating it if necessary.
	 * @param context
	 * @param root the root directory of the cache
	 */
	public static XDSStudyCache getInstance(String context, File root, Element element) {
		XDSStudyCache cache = caches.get(context);
		if (cache == null) {
			cache = new XDSStudyCache(context, root, element);
			caches.put(context, cache);
		}
		return cache;
	}

	/**
	 * Get the current singleton instance of the XDSStudyCache,
	 * for a specified context, returning null if it does not exist.
	 * @param context of the requested cache.
	 */
	public static XDSStudyCache getInstance(String context) {
		return caches.get(context);
	}

	/**
	 * Close the cache and its underlying database.
	 */
	public void close() {
		database.close();
	}

	/**
	 * Determine whether the cache has been closed.
	 * This method ensures that any studies that are in transit
	 * are completed before returning.
	 */
	public boolean isClosed() {
		if (!database.isClosed()) return false;

		//TODO: check whether anything is in transit.
		return true; //******************************** TEST
	}

	/**
	 * Store an object in the cache, updating the database with
	 * PHI obtained from a pre-anonymized version of the object.
	 * @param fileObject the object to be stored
	 * @param phiObject the object from which PHI is to be obtained
	 * for storage in the database. If this parameter is null, the
	 * indexed values are obtained from fileObject.
	 */
	public void store(FileObject fileObject, FileObject phiObject) {

		//In this method, fileObject refers to the object that has
		//flowed down the pipe to the export stage. Depending on the
		//configuration, that object has probably been anonymized.

		//The phiObject is the object that was cached by the ObjectCache
		//stage before any anonymization has taken place, so it contains PHI.

		//Thus, fileObject is the object that will be exported, while
		//phiObject is the object containing the PHI that will be presented to the
		//user who selects studies (through the servlet) and assigns the
		//destination key, triggering the actual export.

		//Note that if the phiObject is null (say, because the configuation
		//is incorrect or because the stage is intentionally being used to
		//export objects containing PHI), we use the anonymized values
		//in the database. This causes no problems for the program, but
		//administrators should be warned that this can be a PHI leak.

		FileObject fo = (phiObject != null) ? phiObject : fileObject;
		String studyUID = fo.getStudyInstanceUID();
		String dirname = studyUID.replaceAll("[\\\\/\\s]", "_").trim();
		File studyDir = new File(cacheRoot, dirname);
		studyDir.mkdirs();
		String filename = fo.getSOPInstanceUID().replaceAll("[\\\\/\\s]", "_").trim();
		File file = new File(studyDir, filename);
		int inc = file.exists() ? 0 : 1;
		FileUtil.copy(fileObject.getFile(), file);

		//Now update the database so the servlet can track the study.
		XDSStudy study = database.get(studyUID);
		if (study == null) {
			//There is no study for this object, create a new study.
			study = new XDSStudy(fo, studyDir);
		}
		else {
			//The study exists. Update the modality if it isn't already there
			if (study.getModality().equals("") && (fo instanceof DicomObject)) {
				study.setModality( ((DicomObject)fo).getModality() );
			}
		}
		study.setSize(studyDir.listFiles().length); //count the object added to the study
		study.setLastModifiedTime(); //record the time of this object storage
		study.setStatus(XDSStudyStatus.OPEN);
		database.put(study);
	}

	/**
	 * Get the number of studies that have the XDSStudyStatus COMPLETE.
	 */
	public int getCompleteStudyCount() {
		return database.getCompleteStudyCount();
	}

	/**
	 * Get the total number of studies in the database.
	 */
	public int getStudyCount() {
		return database.getStudyCount();
	}

	/**
	 * Get the database entries for studies that are either OPEN or COMPLETE,
	 * sorted on PatientID.
	 */
	public Document getActiveStudiesXML() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Studies");
			doc.appendChild(root);
			XDSStudy[] studies = database.getActiveStudies();
			for (XDSStudy study : studies) {
				root.appendChild( doc.importNode(study.getXML().getDocumentElement(), true) );
			}
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the active studies");
			return null;
		}
	}

	/**
	 * Get the database entries for studies that have the status INTRANSIT, SUCCESS or FAILED,
	 * sorted on PatientID.
	 */
	public Document getSentStudiesXML() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Studies");
			doc.appendChild(root);
			XDSStudy[] studies = database.getSentStudies();
			for (XDSStudy study : studies) {
				root.appendChild( doc.importNode(study.getXML().getDocumentElement(), true) );
			}
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the active studies");
			return null;
		}
	}

	/**
	 * Get the database entry for a study in XML format.
	 * @param studyUID the StudyInstanceUID of the study.
	 */
	public Document getStudyXML(String studyUID) {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Studies");
			doc.appendChild(root);
			XDSStudy study = database.get(studyUID);
			if (study != null) {
				root.appendChild( doc.importNode(study.getXML().getDocumentElement(), true) );
			}
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the requested study");
			return null;
		}
	}

	/**
	 * Change the status of all studies which have the XDSStudyStatus OPEN
	 * and which are older than a specified time.
	 * @param time the latest lastModifiedTime of a study which is to be modified.
	 */
	public void checkOpenStudies(long time) {
		XDSStudy[] studies = database.getStudies(XDSStudyStatus.OPEN);
		for (XDSStudy study : studies) {
			if (study.getLastModifiedTime() < time) {
				study.setStatus(XDSStudyStatus.COMPLETE);
				database.put(study);
			}
		}
	}

	/**
	 * Remove all studies which have the XDSStudyStatus SUCCESS
	 * and are older than a specified time.
	 * @param time the latest lastModifiedTime of a study which is to be removed.
	 */
	public void deleteTransmittedStudies(long time) {
		XDSStudy[] studies = database.getStudies(XDSStudyStatus.SUCCESS);
		for (XDSStudy study : studies) {
			if (study.getLastModifiedTime() < time) {
				File dir = study.getDirectory();
				FileUtil.deleteAll(dir);
				database.remove(study);
			}
		}
	}

	/**
	 * Delete a study.
	 * @param studyUID the UID of the study to be queued.
	 */
	public void deleteStudy(String studyUID) {
		XDSStudy study = database.get(studyUID);
		if (study != null) database.remove(study);
	}

	/**
	 * Enqueue a study for transmission, changing its status to QUEUED.
	 * @param key the key identifying the destination.
	 * @param studyUID the UID of the study to be queued.
	 */
	public void sendStudy(String key, String studyUID) {
		XDSStudy study  = database.get(studyUID);
		if (study != null) {
			study.setDestination(key);
			study.setStatus( XDSStudyStatus.QUEUED );
			database.put(study);
			execSvc.execute( new StudySender(study) );
		}
	}

	//The thread that sends studies
	class StudySender extends Thread implements XdsSubmissionListener {

		XDSStudy study;
		Timer timer = null;

		public StudySender(XDSStudy study) {
			this.study = study;
		}

		public void run() {
			if (study != null) {
				try {
					study.setStatus(XDSStudyStatus.INTRANSIT);
					database.put(study);

					XdsSender sender = new XdsSender(element);
					sender.addXDSSubmissionListener(this);
					timer = new Timer();
					Status status = sender.submit(
										study.getFiles(),
										study.getDestination());
					logger.info("XdsSender.submit returned "+status.toString()+" at "+timer.getTimeString());
					if (status.equals(Status.OK)) {
						study.setStatus( XDSStudyStatus.SUCCESS );
					}
					else {
						study.setStatus( XDSStudyStatus.FAILED );
					}
					database.put(study);
				}
				catch (Exception ex) {
					logger.warn("Unable to transmit "+study.getStudyUID());
				}
			}
			else logger.warn("Attempt to transmit null study");
		}

		public void eventOccurred(XdsSubmissionEvent event) {
			if (event instanceof Iti41Event) {
				int currentImage = ((Iti41Event)event).getCurrentImage();
				logger.info("Iti41Event (currentImage:"+currentImage+") received at "+timer.getTimeString());
				study.setObjectsSent(currentImage);
				database.put(study);
			}
		}
	}

	class Timer {
		long time = 0;
		public Timer() {
			reset();
		}
		public void reset() {
			time = System.currentTimeMillis();
		}
		public long getTime() {
			return System.currentTimeMillis() - time;
		}
		public String getTimeString() {
			return getTime() +" ms";
		}
	}

}
