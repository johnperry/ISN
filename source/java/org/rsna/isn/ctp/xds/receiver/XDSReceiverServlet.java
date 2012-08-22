/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
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
 * A servlet to allow manual selection of submission sets to
 * retrieve from the clearinghouse.
 */
public class XDSReceiverServlet extends Servlet {

	static final Logger logger = Logger.getLogger(XDSReceiverServlet.class);

	/**
	 * Static init method. Nothing is required; the empty
	 * method here is just to prevent the superclass' method
	 * from creating an unnecessary index.html file.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a XDSReceiverServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public XDSReceiverServlet(File root, String context) {
		super(root, context);
	}

	/**
	 * Handle requests for the management page.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
		Path path = req.getParsedPath();
		int length = path.length();

		if (req.isFromAuthenticatedUser()) {

			if (length == 1) {
				//This is a request for the main page
				res.write( getPage() );
				res.setContentType("html");
				res.disableCaching();
				res.send();
				return;
			}
		}

		//None of the above, treat it as a file request.
		super.doGet(req, res);
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		String usertoken = req.getParameter("usertoken", "usertoken");
		String dateofbirth = req.getParameter("dateofbirth", "19460201");
		String password = req.getParameter("password", "password");

		String key = TransHash.gen(usertoken, dateofbirth, password);

		//For now, just return the key
		res.write("Key: "+key);
		res.setContentType("txt");
		res.send();
	}

	private String getPage() {
		try {
			Document doc = getSubmissionSetsDocument();
			String xslPath = "/XDSReceiverServlet.xsl";
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslPath ) );
			return XmlUtil.getTransformedText( doc, xsl, null );
		}
		catch (Exception ex) { return "Unable to create the receiver page."; }
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
