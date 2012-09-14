/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import java.util.List;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.pipeline.PipelineStage;
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

		if (req.userHasRole("import")) {

			if (length == 1) {
				//This is a request for the main page
				res.write( getPage("", "", "", "", null) );
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

		//Only accept connections from users with the import privilege
		if (!req.userHasRole("import")) { res.redirect("/"); return; }

		String usertoken = req.getParameter("usertoken", "usertoken").trim();
		String dateofbirth = req.getParameter("dateofbirth", "19460201").trim();
		String password = req.getParameter("password", "password").trim();
		String key = TransHash.gen(usertoken, dateofbirth, password);

		Configuration config = Configuration.getInstance();
		XDSImportService xdsImportService = (XDSImportService)config.getRegisteredStage(context);
        List<DocumentInfo> docInfoList = xdsImportService.getSubmissionSets(key);

		res.write( getPage(usertoken, dateofbirth, password, key, docInfoList) );
		res.setContentType("html");
		res.send();
	}

	private String getPage(String token, String dob, String pw, String key, List<DocumentInfo> docInfoList) {
		try {
			Document doc = getStudiesDocument(docInfoList);
			String xslPath = "/XDSReceiverServlet.xsl";
			Object[] params = {
				"token", token,
				"dob", dob,
				"pw", pw,
				"key", key,
			};
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslPath ) );
			return XmlUtil.getTransformedText( doc, xsl, params );
		}
		catch (Exception ex) { return "Unable to create the receiver page."; }
	}

	private Document getStudiesDocument(List<DocumentInfo> docInfoList) {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Studies");
			doc.appendChild(root);
			if (docInfoList != null) {
				for (DocumentInfo info : docInfoList) {
					Element study = doc.createElement("Study");
					root.appendChild(study);
					study.setAttribute("patientName", fixString(info.getPatientName()));
					study.setAttribute("studyDate", fixDate(fixString(info.getStudyDate())));
					study.setAttribute("studyUID", info.getStudyInstanceUID());

					//Try to find something for the studyDescription
					String sd = fixString(info.getStudyDescription());
					if (sd.equals("")) sd = fixString(info.getModality());
					study.setAttribute("studyDescription", sd);
				}
			}
			return doc;
		}
		catch (Exception ex) {
			logger.warn("Unable to create the Studies Document", ex);
			return null;
		}
	}

	private String fixString(String s) {
		return (s == null) ? "" : s.trim();
	}

	private String fixDate(String date) {
		if (date.length() == 8) {
			return date.substring(0,4) + "."
					+ date.substring(4,6) + "."
						+ date.substring(6);
		}
		return "";
	}

}
