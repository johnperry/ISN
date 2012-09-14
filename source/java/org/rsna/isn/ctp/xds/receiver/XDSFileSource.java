/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.CDATASection;

/**
 * A class to obtain files from an Image Sharing Project Clearinghouse.
 */
public class XDSFileSource {

	static final Logger logger = Logger.getLogger(XDSFileSource.class);

	File dir; //A temp directory for internal use
	Element element; //The confif file element for the parent stage
	File queue = null;
	DocSetDB docsetDB = null;

	RetrieveDocuments retrieveDocuments;
	List<DocumentInfo> docInfoList;
	String siteID;

	boolean test = false;

    /**
	 * Construct an XDSFileSource.
	 * @param element the configuration element of the parent stage
	 * @param dir a directory for use in receiving files
	 */
	public XDSFileSource(Element element, File dir, String siteID)  throws Exception {
		this.element = element;
		this.dir = dir;
		this.test = element.getAttribute("test").equals("yes");

		File dbdir = new File(dir, "database");
		docsetDB = new DocSetDB(dbdir, test);

		queue = new File(dir, "queue");
		queue.mkdirs();

		//Get the site ID that identifies the site to the clearinghouse
		if (siteID == null) siteID = element.getAttribute("siteID");
		this.siteID = siteID;

		retrieveDocuments = new RetrieveDocuments(queue, docsetDB, siteID);
	}

	/**
	 * Close the database
	 */
	public void shutdown() {
		docsetDB.close();
	}

	/**
	 * Get studies from the clearinghouse.
	 * @return the file, or null if no file is available
	 */
	public File getFile() {
		File outFile = null;
		try {
			File[] files = queue.listFiles();
			if (files.length == 0) {
				//get KOS and report for studies under this siteID
				Timer t = new Timer();
				docInfoList = retrieveDocuments.getSubmissionSets();

				//get images for each study
				int numOfDocs = 0;
				for (DocumentInfo docInfo : docInfoList) {
					logger.debug("About to retrieve study: "+docInfo.getDocumentUniqueID());
					numOfDocs += retrieveDocuments.getStudy(docInfo);
					logger.debug("...done retrieving "+docInfo.getDocumentUniqueID());
				}
				logger.debug("-----------------Clearinghouse polling complete: "+t.getElapsedTime());

				if (numOfDocs > 0){
					files = queue.listFiles();
					if (files.length >0 ) {
						outFile = files[0];
					}
				}

				if (test) {
					try { Thread.sleep(60000); }
					catch (Exception ignore) { }
				}
			}
			else {
				outFile = files[0];
			}
		} catch (Exception e) {
			logger.error("XDSFileSource Error: " + e.getMessage(), e);
		}
		return outFile;
	}
}