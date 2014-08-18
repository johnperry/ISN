/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import java.util.GregorianCalendar;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.ZipObject;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A servlet to provide a UI for computing keys.
 */
public class XDSToolServlet extends Servlet {

	static final Logger logger = Logger.getLogger(XDSToolServlet.class);

	/**
	 * Static init method. Nothing is required; the empty
	 * method here is just to prevent the superclass' method
	 * from creating an unnecessary index.html file.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a XDSToolServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public XDSToolServlet(File root, String context) {
		super(root, context);
	}

	/**
	 * Handle requests for the page.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
		Path path = req.getParsedPath();
		int length = path.length();

		if (length == 1) {
			//This is a request for the main page
			res.write( getPage("", "", "", "") );
			res.setContentType("html");
			res.disableCaching();
			res.send();
			return;
		}

		//Not a page request, treat it as a file request.
		super.doGet(req, res);
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		String deftoken = Long.toString( System.currentTimeMillis() );
		String usertoken = req.getParameter("usertoken", deftoken).trim();
		String dateofbirth = req.getParameter("dateofbirth", "19460201").trim();
		String password = req.getParameter("password", "password").trim();

		dateofbirth = fixDOB(dateofbirth);

		String key = TransHash.gen(usertoken, dateofbirth, password);

		if (req.hasParameter("usertoken")) {
			res.write( getPage(usertoken, dateofbirth, password, key) );
		}
		else {
			res.write( getPage("", "", "", key) );
		}

		res.setContentType("html");
		res.send();
	}

	private String fixDOB(String dob) {
		String[] x = dob.split("/");
		if (x.length == 3) {
			int m = StringUtil.getInt(x[0]);
			int d = StringUtil.getInt(x[1]);
			int y = StringUtil.getInt(x[2]);
			dob = String.format( "%04d%02d%02d", y, m, d);
		}
		return dob;
	}

	private String getPage(String token, String dob, String pw, String key) {
		try {
			//Get today's date
			GregorianCalendar cal = new GregorianCalendar();
			String today = String.format( "%04d%02d%02d",
											cal.get(cal.YEAR),
											(cal.get(cal.MONTH)+1),
											cal.get(cal.DAY_OF_MONTH) );

			//Make some random tokens
			StringBuffer tokens = new StringBuffer();
			for (int i=0; i<25; i++) {
				tokens.append(
					org.apache.commons.lang.RandomStringUtils.random(6, "ybndrfg8ejkmcpqxotluwisza34h769")
					+ " ");
			}

			//Make the page
			Document doc = getSubmissionSetsDocument();
			String xslPath = "/XDSToolServlet.xsl";
			String[] params = {
					"today", today,
					"token", token,
					"dob", dob,
					"pw", pw,
					"key", key,
					"tokens", tokens.toString()
				};
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslPath ) );
			return XmlUtil.getTransformedText( doc, xsl, params );
		}
		catch (Exception ex) {
			logger.warn(ex);
			return "Unable to create the key tool page.";
		}
	}

	private Document getSubmissionSetsDocument() {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("SubmissionSets");
			doc.appendChild(root);
			return doc;
		}
		catch (Exception ex) {
			return null;
		}
	}

}
