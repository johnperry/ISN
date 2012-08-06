/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
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
			boolean admin = req.userHasRole("admin");
			boolean update = req.userHasRole("update");

			if (length == 1) {
				//This is a request for the main page
				boolean isEdgeServer = req.getParameter("ui","").equals("ES");
				res.write( getPage(admin, update, isEdgeServer) );
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
		//For now, just send a notimplemented response
		res.setResponseCode( res.notimplemented );
		res.send();
	}

	private String getPage(boolean admin, boolean update, boolean isEdgeServer) {
		try {
			Document doc = getSubmissionSetsDocument();
			String xslPath = isEdgeServer ? "/XDSReceiverServletES.xsl" : "/XDSReceiverServlet.xsl";
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslPath ) );
			String[] params = new String[] {
				"admin", (admin ? "yes" : "no"),
				"update", (update ? "yes" : "no"),
				"isEdgeServer", (isEdgeServer ? "yes" : "no")
			};
			return XmlUtil.getTransformedText( doc, xsl, params );
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
