/* Copyright (c) <2010>, <Radiological Society of North America>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Neither the name of the <RSNA> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package org.rsna.isn.ctp.xds.sender.test;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.rsna.isn.ctp.xds.sender.XdsSender;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionEvent;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;
import org.w3c.dom.Document;

/**
 * Utility class for testing the XDS submission process without a full CTP installation
 * 
 * @author Wyatt Tellis
 * @version 3.0.0
 */
public class TestXdsSender
{
	private static final Logger logger;

	static
	{
		// Set as needed for testing
		System.setProperty("log4j.debug", "true");
		System.setProperty("log4j.configuration", "file:/D:/rsna/conf/ctp-xds-sender-log4j.properties");
		logger = Logger.getLogger(TestXdsSender.class);


		System.setProperty("javax.net.ssl.keyStore", "D:\\rsna\\conf\\keystore.jks");
		System.setProperty("javax.net.ssl.keyStorePassword", "edge1234");

		System.setProperty("javax.net.ssl.trustStore", "D:\\rsna\\conf\\truststore.jks");
		System.setProperty("javax.net.ssl.trustStorePassword", "edge1234");


		System.setProperty("xds-dump-dir", "D:\\rsna");
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		try
		{
			//
			// Load fake configuration
			//

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputStream is = TestXdsSender.class.getResourceAsStream("test-config.xml");
			Document document = builder.parse(is);
			is.close();



			XdsSender sender = new XdsSender(document.getDocumentElement());

			XdsSubmissionListener listener = new XdsSubmissionListener()
			{
				@Override
				public void eventOccurred(XdsSubmissionEvent event)
				{
					logger.info(event);
				}

			};
			sender.addXDSSubmissionListener(listener);


			// Generate hash for testing with Wendy's retrieve content app.
			// See: https://docs.google.com/document/d/1BpI222-Dx2NU0jwL5FqqO7hBmE9vG1elI1Zz9l8eIX0/edit#
			String token = RandomStringUtils.random(6, "ybndrfg8ejkmcpqxotluwisza34h769");
			String dob = "19970418";
			String password = "test1234";

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(token.getBytes("UTF-8"));
			md.update(dob.getBytes("UTF-8"));
			md.update(password.getBytes("UTF-8"));


			String hash = new BigInteger(1, md.digest()).toString(16);

			File dir = new File("D:\\rsna\\ctp");			
			List<File> files = Arrays.asList(dir.listFiles());
			sender.submit(files, hash);

			logger.info("Successfully submitted files using the following credentials: \r\n\t"
					+ "Exam ID:  " + token + "\r\n\t"
					+ "DOB:      " + dob + "\r\n\t"
					+ "Password: " + password + "\r\n\t"
					+ "Hash:     " + hash + "\r\n\t");
		}
		catch (Exception ex)
		{
			logger.fatal("Unable to submit documents", ex);
		}
	}

}
