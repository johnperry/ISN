/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

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
 * A servlet to manage the assignment of cached studies to
 * destination keys and to trigger their transmission.
 */
public class XDSSenderServlet extends Servlet {

	static final Logger logger = Logger.getLogger(XDSSenderServlet.class);

	/**
	 * Static init method. Nothing is required; the empty
	 * method here is just to prevent the superclass' method
	 * from creating an unnecessary index.html file.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a XDSSenderServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public XDSSenderServlet(File root, String context) {
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
				//This is a request for the management page
				boolean isEdgeServer = req.getParameter("ui","").equals("ES");
				res.write( getPage(admin, update, isEdgeServer) );
				res.setContentType("html");
				res.disableCaching();
				res.send();
				return;
			}

			else if (path.element(1).equals("status")) {
				//This is a request for status information in XML format
				String studyUID = req.getParameter("study", "");
				XDSStudyCache cache = XDSStudyCache.getInstance(context);
				if (cache == null) {
					//There is no cache for this context, send an empty response
					res.write( "<Studies/>" );
				}
				else {
					Document doc = null;
					if (studyUID.equals("")) {
						//This is a request for status on all active studies
						doc = cache.getActiveStudiesXML();
						res.write( XmlUtil.toString(doc.getDocumentElement()) );
					}
					else {
						//This is a request for a status on a single study
						doc = cache.getStudyXML(studyUID);
						res.write( XmlUtil.toString(doc.getDocumentElement()) );
					}
				}
				res.setContentType("xml");
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

		//Only accept connections from users with the update privilege
		if (!req.userHasRole("update")) { res.redirect("/"); return; }

		//Reload the page so the user can see what he did.
		boolean isEdgeServer = req.getParameter("ui","").equals("ES");
		res.redirect("/" + context + (isEdgeServer ? "?ui=ES" : ""));
	}

	private String getPage(boolean admin, boolean update, boolean isEdgeServer) {
		try {
			Destinations destinations = Destinations.getInstance(context);
			Document destinationsDoc = destinations.getDestinationsXML();
			XDSStudyCache cache = XDSStudyCache.getInstance(context);
			Document doc = cache.getActiveStudiesXML();
			String xslPath = isEdgeServer ? "/XDSSenderServletES.xsl" : "/XDSSenderServlet.xsl";
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslPath ) );
			Object[] params = new Object[] {
				"admin", (admin ? "yes" : "no"),
				"update", (update ? "yes" : "no"),
				"isEdgeServer", (isEdgeServer ? "yes" : "no"),
				"destinations", destinationsDoc
			};
			return XmlUtil.getTransformedText( doc, xsl, params );
		}
		catch (Exception ex) { return "Unable to create the sender page."; }
	}

}
