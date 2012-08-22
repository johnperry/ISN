/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import org.apache.log4j.Logger;
import org.rsna.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A database to store entries containing key data about cached studies.
 */
public class XDSDatabase {

	static final Logger logger = Logger.getLogger(XDSDatabase.class);

	private static final String databaseName = "XDSDatabase";
	private static final String studiesHTreeName = "studies";
	private static final String destinationsHTreeName = "destinations";
	private RecordManager recman = null;
	private HTree studies = null;

	private File indexRoot;

	/**
	 * Construct an XDSDatabase.
	 * @param indexRoot the directory within which the database can store its files,
	 */
	public XDSDatabase(File indexRoot) {
		this.indexRoot = indexRoot;
		loadDatabase(indexRoot);
	}

	/**
	 * Get a study from the database.
	 * @param studyUID the UID of the study to fetch
	 * @return the study or null if the studyUID does
	 * not exist in the database.
	 */
	public synchronized XDSStudy get(String studyUID) {
		try { return (XDSStudy)studies.get(studyUID); }
		catch (Exception ex) { return null; }
	}

	/**
	 * Insert a study by its StudyInstanceUID
	 * @param study the study to store. Note: the
	 * studyUID key is obtained from the XDSStudy object.
	 */
	public synchronized void put(XDSStudy study) {
		try {
			studies.put(study.getStudyUID(), study);
			recman.commit();
		}
		catch (Exception ex) {
			logger.warn("Unable to update the study database", ex);
		}
	}

	/**
	 * Remove a study
	 * @param study the study to remove. Note: the
	 * studyUID key is obtained from the XDSStudy object.
	 */
	public synchronized void remove(XDSStudy study) {
		try {
			studies.remove(study.getStudyUID());
			recman.commit();
		}
		catch (Exception ex) {
			logger.warn("Unable to remove the study", ex);
		}
	}

	/**
	 * Get an array of studies that are either OPEN or COMPLETE,
	 * sorted on PatientID.
	 * @return the sorted list of OPEN or COMPLETE studies, or
	 * an empty array if an error occurs.
	 */
	public synchronized XDSStudy[] getActiveStudies() {
		try {
			XDSStudy study;
			String key;
			LinkedList<XDSStudy> list = new LinkedList<XDSStudy>();
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) {
				study = (XDSStudy)studies.get(key);
				XDSStudyStatus status = study.getStatus();
				if (status.is(XDSStudyStatus.OPEN) || status.is(XDSStudyStatus.COMPLETE)) {
					list.add( study );
				}
			}
			XDSStudy[] array = new XDSStudy[list.size()];
			array = list.toArray(array);
			Arrays.sort(array);
			return array;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the active studies", ex);
			return new XDSStudy[0];
		}
	}

	/**
	 * Get an array of studies that have the status QUEUED, INTRANSIT, SUCCESS or FAILED,
	 * sorted on PatientID.
	 * @return the sorted list of QUEUED, INTRANSIT, SUCCESS or FAILED studies, or
	 * an empty array if an error occurs.
	 */
	public synchronized XDSStudy[] getSentStudies() {
		try {
			XDSStudy study;
			String key;
			LinkedList<XDSStudy> list = new LinkedList<XDSStudy>();
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) {
				study = (XDSStudy)studies.get(key);
				XDSStudyStatus status = study.getStatus();
				if (status.is(XDSStudyStatus.QUEUED)
					|| status.is(XDSStudyStatus.INTRANSIT)
						|| status.is(XDSStudyStatus.SUCCESS)
							|| status.is(XDSStudyStatus.FAILED)) {
					list.add( study );
				}
			}
			XDSStudy[] array = new XDSStudy[list.size()];
			array = list.toArray(array);
			Arrays.sort(array);
			return array;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the sent studies", ex);
			return new XDSStudy[0];
		}
	}

	/**
	 * Get an array of all the studies with a specified StudyDtatus,
	 * sorted on PatientID.
	 * @param studyStatus the status of the studies to be returned.
	 * @return the sorted list of all studies, or
	 * an empty array if an error occurs.
	 */
	public synchronized XDSStudy[] getStudies(XDSStudyStatus studyStatus) {
		try {
			XDSStudy study;
			String key;
			LinkedList<XDSStudy> list = new LinkedList<XDSStudy>();
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) {
				study = (XDSStudy)studies.get(key);
				XDSStudyStatus status = study.getStatus();
				if (status.is(studyStatus)) {
					list.add( study );
				}
			}
			XDSStudy[] array = new XDSStudy[list.size()];
			array = list.toArray(array);
			Arrays.sort(array);
			return array;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the studies with status "+studyStatus.toString(), ex);
			return new XDSStudy[0];
		}
	}

	/**
	 * Get the number of studies that are COMPLETE.
	 */
	public synchronized int getCompleteStudyCount() {
		try {
			XDSStudy study;
			String key;
			int count = 0;
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) {
				study = (XDSStudy)studies.get(key);
				XDSStudyStatus status = study.getStatus();
				if (status.is(XDSStudyStatus.COMPLETE)) {
					count++;
				}
			}
			return count;
		}
		catch (Exception ex) {
			logger.warn("Unable to count the studies with status COMPLETE", ex);
			return 0;
		}
	}

	/**
	 * Get the number of studies that are INTRANSIT.
	 */
	public synchronized int getInTransitStudyCount() {
		try {
			XDSStudy study;
			String key;
			int count = 0;
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) {
				study = (XDSStudy)studies.get(key);
				XDSStudyStatus status = study.getStatus();
				if (status.is(XDSStudyStatus.INTRANSIT)) {
					count++;
				}
			}
			return count;
		}
		catch (Exception ex) {
			logger.warn("Unable to count the studies with status INTRANSIT", ex);
			return 0;
		}
	}

	/**
	 * Get the total number of studies that are in the database.
	 */
	public synchronized int getStudyCount() {
		try {
			String key;
			int count = 0;
			FastIterator it = studies.keys();
			while ( (key = (String)it.next()) != null ) count++;
			return count;
		}
		catch (Exception ex) {
			logger.warn("Unable to count the studies", ex);
			return 0;
		}
	}

	/**
	 * Commit changes and close the index.
	 * No errors are reported and no operations
	 * are available after this call.
	 */
	public synchronized void close() {
		if (recman != null) {
			try {
				recman.commit();
				recman.close();
				recman = null;
				studies = null;
			}
			catch (Exception ignore) { }
		}
	}

	/**
	 * Indicate whether the database is closed.
	 * return true if the database is closed; false otherwise.
	 */
	public synchronized boolean isClosed() {
		return (recman == null);
	}

	//Load the database from the JDBM files, creating the JDBM files if necessary.
	private void loadDatabase(File dir) {
		if (recman == null) {
			try {
				File databaseFile = new File(dir, databaseName);
				recman = getRecordManager(databaseFile.getAbsolutePath());
				studies = getHTree(recman, studiesHTreeName);
			}
			catch (Exception ex) {
				logger.warn("Unable to instantiate the XDS studies database.", ex);
				studies = null;
			}
		}
	}

	//Get a RecordManager
	private RecordManager getRecordManager(String filename) throws Exception {
		Properties props = new Properties();
		props.put( RecordManagerOptions.THREAD_SAFE, "true" );
		return RecordManagerFactory.createRecordManager( filename, props );
	}

	//Get a named HTree, or create it if it doesn't exist.
	private HTree getHTree(RecordManager recman, String name) throws Exception {
		HTree index = null;
		long recid = recman.getNamedObject(name);
		if ( recid != 0 )
			index = HTree.load( recman, recid );
		else {
			index = HTree.createInstance( recman );
			recman.setNamedObject( name, index.getRecid() );
			recman.commit();
		}
		return index;
	}

}
