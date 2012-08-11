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
public class Destination implements Serializable, Comparable<Destination> {

	static final long serialVersionUID = 1L;

	static final Logger logger = Logger.getLogger(Destination.class);

	String key;
	String name;

	/**
	 * Construct a Destination.
	 * @param key the clearinghouse key for submission sets for this destination.
	 * @param name the meaningful name of the destination (e.g. Mayo XYZ Trial).
	 */
	public Destination(String key, String name) {
		this.key = key;
		this.name = name;
	}

	/**
	 * Get the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the name
	 */
	public String getName() {
		return name;
	}

	public Document getXML() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Destination");
			doc.appendChild(root);
			root.setAttribute("key", key);
			root.setAttribute("name", name);
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to create the XML for a study");
			return null;
		}
	}

	public int compareTo(Destination d) {
		return name.compareTo(d.getName());
	}

}
