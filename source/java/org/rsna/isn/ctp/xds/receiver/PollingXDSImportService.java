/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import java.net.URL;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.ImportService;
import org.rsna.ctp.pipeline.QueueManager;
import org.rsna.server.HttpServer;
import org.rsna.server.ServletSelector;
import org.rsna.util.StringUtil;
import org.w3c.dom.Element;

/**
 * An ImportService that polls an XSDFileSource to obtain files.
 */
public class PollingXDSImportService extends AbstractPipelineStage implements ImportService {

	static final Logger logger = Logger.getLogger(PollingXDSImportService.class);

	File active = null;
	String activePath = "";
	File temp = null;
	QueueManager queueManager = null;
	int count = 0;

	long lastPollTime = 0L;
	int interval = 60;

	URL url = null;
	String siteID;
	XDSFileSource fileSource = null;

	Poller poller = null;

	String servletContext = "";

	/**
	 * Construct a PollingXDSImportService.
	 * @param element the XML element from the configuration file,
	 * specifying the configuration of the stage.
	 */
	public PollingXDSImportService(Element element) throws Exception {
		super(element);
		if (root == null)
			logger.error(name+": No root directory was specified.");
		else {
			temp = new File(root, "xds");
			temp.mkdirs();
			File queue = new File(root, "queue");
			queueManager = new QueueManager(queue, 0, 0); //use default settings
			active = new File(root, "active");
			active.mkdirs();
			activePath = active.getAbsolutePath();
			queueManager.enqueueDir(active); //requeue any files that are left from an ungraceful shutdown.

			//Get the minimum polling interval (in seconds)
			interval = Math.max(  StringUtil.getInt(element.getAttribute("interval"), interval), interval );
			interval *= 1000;

			//Get the servlet context. This is only used for the clinical receiver, not the research receiver
			servletContext = element.getAttribute("servletContext").trim();
		}
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
			//Instantiate the XDSFileSource
			fileSource = new XDSFileSource(element, temp, null);

			//Instantiate the Poller and start it.
			poller = new Poller();
			poller.start();

			//Install the servlet if the context is supplied
			if (!servletContext.equals("")) {
				HttpServer server = config.getServer();
				ServletSelector selector = server.getServletSelector();
				selector.addServlet(servletContext, XDSReceiverServlet.class);
			}
		}
		catch (Exception ex) {
			logger.warn("Unable to start the stage", ex);
		}
	}

	/**
	 * Stop the pipeline stage.
	 */
	public void shutdown() {
		stop = true;
		if (poller != null) poller.interrupt();
	}

	/**
	 * Determine whether the pipeline stage has shut down.
	 */
	public boolean isDown() {
		if (poller != null) return !poller.isAlive();
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
			+ "<td>" + ((queueManager!=null) ? queueManager.size() : "???") + "</td></tr>"
			+ "<tr><td width=\"20%\">Last poll time:</td>"
			+ "<td>" + StringUtil.getTime(lastPollTime,":") + "</td></tr>";
		return super.getStatusHTML(stageUniqueStatus);
	}

	//The class to poll the XDSFileSource and enqueue files.
	class Poller extends Thread {
		public Poller() { }

		public void run() {
			File file;
			while (!stop) {
				if ( (file=fileSource.getFile()) != null ) {
					queueManager.enqueue(file);
					file.delete();
					count++;
					lastFileIn = file;
					lastTimeIn = System.currentTimeMillis();
				}
				else {
					try { Thread.sleep(interval); }
					catch (Exception ex) { }
				}
			}
			fileSource.shutdown();
		}
	}

}
