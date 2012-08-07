/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.FileObject;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A cache for studies.
 */
public class XDSStudyCache {

	static final Logger logger = Logger.getLogger(XDSStudyCache.class);

	private static Hashtable<String,XDSStudyCache> caches = new Hashtable<String,XDSStudyCache>();
	private File cacheRoot;
	private File indexRoot;
	private String context;
	private XDSDatabase database;

	/**
	 * Construct an XDSStudyCache.
	 * @param context
	 * @param root the root directory of the cache
	 */
	protected XDSStudyCache(String context, File root ) {
		this.context = context;
		this.cacheRoot = new File(root, "cache");
		cacheRoot.mkdirs();
		this.indexRoot = new File(root, "index");
		indexRoot.mkdirs();
		this.database = new XDSDatabase(indexRoot);
	}

	/**
	 * Get the current singleton instance of the XDSStudyCache
	 * for a specified context. creating it if necessary.
	 * @param context
	 * @param root the root directory of the cache
	 */
	public static XDSStudyCache getInstance(String context, File root) {
		XDSStudyCache cache = caches.get(context);
		if (cache == null) {
			cache = new XDSStudyCache(context, root);
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
		//flowed down the pipe to the XDSExportService stage. That
		//object has probably been anonymized.
		//The phiObject is the object that was cached by the ObjectCache
		//stage before any anonymization has taken place, so it contains PHI.
		//Thus, fileObject is the object that will be exported, while
		//phiObject is the object containing the PHI that will be presented to the
		//user who selects studies (through the servlet) and assigns the
		//destination key, triggering the actual export.
		//Note that if the phiObject is null (say, because the configuation
		//is incorrect or because the stage is intentionally being used to
		//export objects containing PHI), we use the anonymized values
		//in the database. This causes no problems for the program.
		FileObject fo = (phiObject != null) ? phiObject : fileObject;
		String studyUID = fo.getStudyInstanceUID();
		String dirname = studyUID.replaceAll("[\\\\/\\s]", "_").trim();
		File studyDir = new File(cacheRoot, dirname);
		studyDir.mkdirs();
		String filename = fo.getSOPInstanceUID().replaceAll("[\\\\/\\s]", "_").trim();
		File file = new File(studyDir, filename);
		int inc = file.exists() ? 0 : 1;
		FileUtil.copy(fileObject.getFile(), file);

		//Now update the database so the servlet can know about the study.
		XDSStudy study = database.get(studyUID);
		if (study == null) {
			study = new XDSStudy(studyUID,
								 studyDir, //where all the objects for the study are stored
								 0, //number of objects received for the study (initialize to zero)
								 0, //number of objects sent to the Clearinghouse (initialize to zero)
								 0, //last time an object was received for the study (will be updated below)
								 XDSStudyStatus.OPEN, //whether the study has met the timeout for readiness
								 null, //the destination key
								 fo.getPatientID(),
								 fo.getPatientName());
		}
		study.incrementSize(inc); //count the object added to the study
		study.setLastModifiedTime(); //record the time of this object storage
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

}
