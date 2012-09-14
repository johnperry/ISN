/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import org.apache.log4j.Logger;
import org.rsna.util.JdbmUtil;
import jdbm.RecordManager;
import jdbm.htree.HTree;

/**
 * A class to keep an index of document set IDs.
 */
public class DocSetDB {

	static final Logger logger = Logger.getLogger(DocSetDB.class);

	File dir = null; //A temp directory for internal use
	RecordManager recman = null;
	HTree index = null;
	boolean acceptAlways = false;

	/**
	 * Construct a DocSetDB.
	 * @param dir the directory in which to create the database
	 */
	public DocSetDB( File dir, boolean acceptAlways) {
		this.dir = dir;
		this.acceptAlways = acceptAlways;
		dir.mkdirs();
		File db = new File(dir, "docsetIDs");
		getIndex(db.getPath());
	}

	/**
	 * Add a document set ID to the database.
	 */
	public void addID(String id) {
		try {
			Long time = new Long( System.currentTimeMillis() );
			index.put( id, time );
                        recman.commit();
		}
		catch (Exception ignore) { }
	}

	/**
	 * See if an document set ID is in the database.
	 */
	public boolean contains(String id) {
		if (acceptAlways) return false;
		try { return (index.get(id) != null); }
		catch (Exception notThere) { return false; }
	}

	/**
	 * Commit and close the database.
	 */
	public void close() {
		if (recman != null) {
			try {
				recman.commit();
				recman.close();
			}
			catch (Exception ex) {
				logger.debug("Unable to commit and close the database");
			}
		}
	}

	//Load the index HTree
	private void getIndex(String indexPath) {
		try {
			recman = JdbmUtil.getRecordManager( indexPath );
			index = JdbmUtil.getHTree( recman, "index" );
		}
		catch (Exception ex) {
			recman = null;
			logger.warn("Unable to load the document set database.");
		}
	}

}