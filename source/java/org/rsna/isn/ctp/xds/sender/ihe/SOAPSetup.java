/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender.ihe;

import java.io.File;
import org.apache.log4j.Logger;
import org.openhealthtools.ihe.atna.auditor.IHEAuditor;
import org.openhealthtools.ihe.atna.auditor.XDSAuditor;

public class SOAPSetup {

	static final Logger logger = Logger.getLogger(SOAPSetup.class);

	/**
	 * Initialize the SOAP parameters if it hasn't already been done.
	 */
	public static void init() {
		String axis2Prop = System.getProperty("axis2.xml");
		if ((axis2Prop == null) || axis2Prop.trim().equals("")) {

			//The property hasn't already been set, so do all the setup.

			XDSAuditor.getAuditor().getConfig().setAuditorEnabled(false);
	        IHEAuditor.getAuditor().getConfig().setAuditorEnabled(false);

			try {
				// Load Axis 2 configuration
				File axis2Xml = new File("axis2.xml");
				System.setProperty("axis2.xml", axis2Xml.getCanonicalPath());
				File soapDir = new File(System.getProperty("java.io.tmpdir"), "ctp-xds-sender");
				soapDir.mkdir();
				System.setProperty("ihe.soap.tmpdir", soapDir.getCanonicalPath());
			}
			catch (Throwable ex) {
				logger.fatal("Unable to initalize Axis 2 configuration.", ex);
				throw new ExceptionInInitializerError(ex);
			}

			System.setProperty("use.http.chunking", "true");
		}
	}
}

