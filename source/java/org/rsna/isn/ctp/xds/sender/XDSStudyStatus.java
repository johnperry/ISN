/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

/**
 * A class to encapsulate typesafe enum return status values for XDS studies
 * being sent to the Clearinghouse. This class provides static final instance
 * for each of the possible operational results.
 */
public class XDSStudyStatus {

	private final String status;

	//Private constructor to prevent anything but this class
	//from instantiating the class.
	private XDSStudyStatus(String status) { this.status = status; }

	/**
	 * Get the text string name of the status class.
	 * @return a text string describing the status instance (OK, FAIL, RETRY).
	 */
	public String toString() { return status; }

	/**
	 * Status value indicating that a study has received an object recently
	 * enough that it cannot yet be considered complete.
	 */
	public static final XDSStudyStatus OPEN = new XDSStudyStatus("OPEN");

	/**
	 * Status value indicating that a study has not received an object recently
	 * and is therefore considered complete.
	 */
	public static final XDSStudyStatus COMPLETE = new XDSStudyStatus("COMPLETE");

	/**
	 * Status value indicating that a user has selected a destination key for
	 * a study and transmission can happen with no further action from the user.
	 */
	public static final XDSStudyStatus INTRANSIT = new XDSStudyStatus("INTRANSIT");

	/**
	 * Status value indicating that a study has been successfully transmitted.
	 */
	public static final XDSStudyStatus SUCCESS = new XDSStudyStatus("SUCCESS");

	/**
	 * Status value indicating that a study transmission failed.
	 */
	public static final XDSStudyStatus FAILED = new XDSStudyStatus("FAILED");

	/**
	 * Status value with no meaning.
	 */
	public static final XDSStudyStatus UNDEFINED = new XDSStudyStatus("UNDEFINED");

	/**
	 * Get the status value corresponding to a string.
	 */
	public static XDSStudyStatus forName(String name) {
		name = name.toUpperCase();
		if (name.equals("OPEN")) return OPEN;
		if (name.equals("COMPLETE")) return COMPLETE;
		if (name.equals("INTRANSIT")) return INTRANSIT;
		if (name.equals("SUCCESS")) return SUCCESS;
		if (name.equals("FAILED")) return FAILED;
		return UNDEFINED;
	}

}