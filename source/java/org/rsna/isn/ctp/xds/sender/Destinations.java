/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.FileObject;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A Hashtable implementation of a database for destinations.
 */
public class Destinations {

	static final Logger logger = Logger.getLogger(Destinations.class);

	private static Hashtable<String,Destinations> contexts = new Hashtable<String,Destinations>();
	private String context;
	private Hashtable<String,Destination> database;

	/**
	 * Construct a Destinations object.
	 * @param context
	 */
	protected Destinations(String context) {
		this.context = context;
		this.database = new Hashtable<String,Destination>();
	}

	/**
	 * Get the current singleton instance of the Destinations
	 * for a specified context. creating it if necessary.
	 * @param context the servlet context under which the
	 * associated servlet is located.
	 */
	public static Destinations getInstance(String context) {
		Destinations destinations = contexts.get(context);
		if (destinations == null) {
			destinations = new Destinations(context);
			contexts.put(context, destinations);
		}
		return destinations;
	}

	/**
	 * Remove all destinations for this context
	 */
	public void clear() {
		database.clear();
	}

	/**
	 * Close the database.
	 */
	public void close() {
		//Not required in this implementation.
		//Would be required if we switch to an
		//actual database implementation.
	}

	/**
	 * Determine whether the database has been closed.
	 * Since this implementation is a Hashtable,
	 * this method always returns true, indicating that
	 * it is acceptable to shut down.
	 */
	public boolean isClosed() {
		return true;
	}

	/**
	 * Store a Destination in the table, indexing by the key.
	 */
	public void put(Destination destination) {
		database.put(destination.key, destination);
	}

	/**
	 * Get the database entries for destinations, sorted on name.
	 */
	public Document getDestinationsXML() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Destinations");
			doc.appendChild(root);
			Destination[] destinations = new Destination[database.size()];
			destinations = database.values().toArray(destinations);
			Arrays.sort(destinations);
			for (Destination destination : destinations) {
				root.appendChild( doc.importNode(destination.getXML().getDocumentElement(), true) );
			}
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to list the destinations");
			return null;
		}
	}

}
