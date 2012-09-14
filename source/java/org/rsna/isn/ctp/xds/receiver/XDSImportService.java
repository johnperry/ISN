/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import java.net.URL;
import java.util.List;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.ImportService;
import org.rsna.ctp.pipeline.QueueManager;
import org.rsna.isn.ctp.ISNRoles;
import org.rsna.isn.ctp.xds.sender.ihe.SOAPSetup;
import org.rsna.server.HttpServer;
import org.rsna.server.ServletSelector;
import org.rsna.util.StringUtil;
import org.w3c.dom.Element;

/**
 * An ImportService that is driven by a servlet to obtain studies from the clearinghouse.
 */
public class XDSImportService extends AbstractPipelineStage implements ImportService {

	static final Logger logger = Logger.getLogger(XDSImportService.class);

	File active = null;
	String activePath = "";
	QueueManager queueManager = null;
	int count = 0;

	DocSetDB docSetDB = null;
	File temp = null;

	String servletContext = "";

	/**
	 * Construct an XDSImportService.
	 * @param element the XML element from the configuration file,
	 * specifying the configuration of the stage.
	 */
	public XDSImportService(Element element) throws Exception {
		super(element);
		if (root == null) logger.error(name+": No root directory was specified.");

		temp = new File(root, "xds");
		temp.mkdirs();
		File queue = new File(root, "queue");
		queueManager = new QueueManager(queue, 0, 0); //use default settings
		active = new File(root, "active");
		active.mkdirs();
		activePath = active.getAbsolutePath();
		queueManager.enqueueDir(active); //requeue any files that are left from an ungraceful shutdown.

		//Get the servlet context. This is only used for the clinical receiver, not the research receiver
		servletContext = element.getAttribute("servletContext").trim();

		//Initialize the SOAP configuration.
		//Note: The static init method only initializes if it hasn't already been done,
		//so in configurations with multiple stages that call this method, the multiple
		//calls don't cause a problem.
		SOAPSetup.init();

		//Set up a dummy DocSetDB so RetrieveDocuments will always accept
		//studies triggered by the XDSSenderServlet
		File dbdir = new File(root, "database");
		docSetDB = new DocSetDB(dbdir, true); //true makes it always return false, indicating that the study has not been seen.

		//Make a temp directory for use by RetrieveDocuments
		temp = new File(root, "temp");
		temp.mkdirs();
	}

	/**
	 * Start the pipeline stage. This method is called by the pipeline
	 * when it is started. At that time, the Configuration object has
	 * been fully constructed, so it can be interrogated. Don't try to
	 * get the Configuration in the constructor of this class.
	 */
	public void start() {
		Configuration config = Configuration.getInstance();

		//Initialize the XDSConfiguration
		XDSConfiguration.load(element);

		try {
			if (!servletContext.equals("")) {
				//Install the servlet
				HttpServer server = config.getServer();
				ServletSelector selector = server.getServletSelector();
				selector.addServlet("isn-tool", XDSToolServlet.class);
				selector.addServlet(servletContext, XDSReceiverServlet.class);

				//Register the stage using the servletContext
				//Note: this must be done here; not in the constructor.
				config.registerStage(this, servletContext);
			}
			else logger.warn(name+": No servlet context was supplied");

			//Install the ISN roles and ensure that the admin user has them.
			ISNRoles.init();
		}
		catch (Exception ex) {
			logger.warn("Unable to start the stage", ex);
		}
	}

	/**
	 * Get the list of submission sets for a key
	 * @param key the hash key identifying the submission sets
	 */
	public List<DocumentInfo> getSubmissionSets(String key) throws Exception {
		RetrieveDocuments rd = new RetrieveDocuments(temp, docSetDB, key);
		return rd.getSubmissionSets();
	}

	/**
	 * Stop the pipeline stage.
	 */
	public void shutdown() {
		stop = true;
	}

	/**
	 * Determine whether the pipeline stage has shut down.
	 */
	public boolean isDown() {
		return stop;
	}

	/**
	 * Get the next object available for processing.
	 * @return the next object available, or null if no object is available.
	 */
	public synchronized FileObject getNextObject() {
		File file;
		if (queueManager != null) {
			while ((file = queueManager.dequeue(active)) != null) {
				lastFileOut = file;
				lastTimeOut = System.currentTimeMillis();
				FileObject fileObject = FileObject.getInstance(lastFileOut);
				fileObject.setStandardExtension();
				return fileObject;
			}
		}
		return null;
	}

	/**
	 * Release a file from the active directory. Note that other stages in the
	 * pipeline may have moved the file, so it is possible that the file will
	 * no longer exist. This method only deletes the file if it is still in the
	 * active directory.
	 * @param file the file to be released, which must be the original file
	 * supplied by the ImportService.
	 */
	public void release(File file) {
		if ((file != null)
				&& file.exists()
					&& file.getParentFile().getAbsolutePath().equals(activePath)) {
			if (!file.delete()) {
				logger.warn("Unable to release the processed file from the active directory:");
				logger.warn("    file: "+file.getAbsolutePath());
			}
		}
	}

	/**
	 * Get HTML text displaying the active status of the stage.
	 * @return HTML text displaying the active status of the stage.
	 */
	public String getStatusHTML() {
		String stageUniqueStatus =
			"<tr><td width=\"20%\">Files received:</td><td>" + count + "</td></tr>"
			+ "<tr><td width=\"20%\">Queue size:</td>"
			+ "<td>" + ((queueManager!=null) ? queueManager.size() : "???") + "</td></tr>";
		return super.getStatusHTML(stageUniqueStatus);
	}

}
