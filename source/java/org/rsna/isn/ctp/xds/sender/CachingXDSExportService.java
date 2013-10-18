/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.net.URL;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.ExportService;
import org.rsna.ctp.stdstages.ObjectCache;
import org.rsna.isn.ctp.ISNRoles;
import org.rsna.isn.ctp.xds.receiver.XDSToolServlet;
import org.rsna.isn.ctp.xds.sender.ihe.SOAPSetup;
import org.rsna.server.HttpServer;
import org.rsna.server.ServletSelector;
import org.rsna.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * An ExportService that caches studies and sends them to the ISN Clearinghouse.
 */
public class CachingXDSExportService extends AbstractPipelineStage implements ExportService {

	static final Logger logger = Logger.getLogger(CachingXDSExportService.class);

	Destinations destinations = null;
	XDSStudyCache studyCache = null;
	String servletContext = "";
	long minAge = 300;
	int count = 0;
	String objectCacheID = "";
	ObjectCache objectCache = null;
	boolean deleteOnTransmission = true;
	MonitorThread monitor = null;
	boolean autosend = false;
	String firstDestinationKey = null;

	/**
	 * Construct a CachingXDSExportService.
	 * @param element the XML element from the configuration file,
	 * specifying the configuration of the stage.
	 */
	public CachingXDSExportService(Element element) throws Exception {
		super(element);
		if (root == null) logger.error(name+": No root directory was specified.");

		root.mkdirs();

		minAge = Math.max( StringUtil.getLong(element.getAttribute("minAge")), minAge ) * 1000;
		deleteOnTransmission = !element.getAttribute("deleteOnTransmission").equals("no");
		autosend = element.getAttribute("autosend").equals("yes");

		//The objectCacheID is the id of a stage that holds the original (PHI) version of an object.
		objectCacheID = element.getAttribute("objectCacheID").trim();

		//Important: the servletContext attribute is used both as the context of the servlet in the
		//server and the index of the studies in the study cache. This makes it possible for the servlet
		//(which knows its context) to obtain the singleton instance of the study cache associated with
		//this stage. If the servletContext is missing, "xds-export" is supplied as the default, but in
		//a configuration with multiple CachingXDSExportService stages, it is important that the stages
		//have different servletContexts, so a warning is issued if no context is supplied.
		servletContext = element.getAttribute("servletContext").trim();
		if (servletContext.equals("")) {
			logger.warn("Missing servletContext, using \"xds-export\".");
			servletContext = "xds-export";
		}
		studyCache = XDSStudyCache.getInstance(servletContext, root, element);

		//Set up the destinations. Like StudyCaches, Destinations are indexed by servletContext.
		destinations = Destinations.getInstance(servletContext);
		destinations.clear();
		NodeList nl = element.getElementsByTagName("Destination");
		for (int i=0; i<nl.getLength(); i++) {
			Element dEl = (Element)nl.item(i);
			Destination d = new Destination( dEl.getAttribute("key").trim(), dEl.getAttribute("name").trim() );
			destinations.put( d );
			if (firstDestinationKey == null) firstDestinationKey = d.getKey();
		}

		//Initialize the SOAP configuration.
		//Note: The static init method only initializes if it hasn't already been done,
		//so in configurations with multiple stages that call this method, the multiple
		//calls don't cause a problem.
		SOAPSetup.init();
	}

	/**
	 * Get the size of the export queue.
	 * @return the number of studies (not objects) in the export queue.
	 */
	public synchronized int getQueueSize() {
		return studyCache.getStudyCount();
	}

	/**
	 * Start the pipeline stage. This method is called by the pipeline
	 * when it is started. At that time, the Configuration object has
	 * been fully constructed, so it can be interrogated. Don't try to
	 * get the Configuration in the constructor of this class.
	 */
	public void start() {
		Configuration config = Configuration.getInstance();

		//Get the ObjectCache stage so we can obtain PHI when necessary.
		objectCache = (ObjectCache)config.getRegisteredStage(objectCacheID);

		//Register this stage under the servlet context so the servlet can find it
		config.registerStage(this);

		//Install the servlet on the context
		HttpServer server = config.getServer();
		ServletSelector selector = server.getServletSelector();
		selector.addServlet(servletContext, XDSSenderServlet.class);
		selector.addServlet("isn-tool", XDSToolServlet.class);

		//Install the ISN roles and ensure that the admin user has them.
		ISNRoles.init();

		//Create and start the monitor thread
		monitor = new MonitorThread();
		monitor.start();
	}

	/**
	 * Stop the pipeline stage.
	 */
	public void shutdown() {
		stop = true;
		studyCache.close();
	}

	/**
	 * Determine whether the pipeline stage has shut down.
	 */
	public boolean isDown() {
		return studyCache.isClosed();
	}

	/**
	 * Add a FileObject to the studyCache.
	 */
	public synchronized void export(FileObject fileObject) {
		count++;
		FileObject phiObject = (objectCache != null) ? objectCache.getCachedObject() : null;
		studyCache.store(fileObject, phiObject);
	}

	/**
	 * Get the active studies.
	 * @return an XML representation of the OPEN or COMPLETE studies managed by this stage.
	 */
	public synchronized Document getActiveStudiesXML() {
		return studyCache.getActiveStudiesXML();
	}

	/**
	 * Get HTML text displaying the active status of the stage.
	 * @return HTML text displaying the active status of the stage.
	 */
	public String getStatusHTML() {
		String stageUniqueStatus =
			"<tr><td width=\"20%\">Files received:</td><td>" + count + "</td></tr>"
			+ "<tr><td width=\"20%\">Studies cached:</td>"
			+ "<td>" + studyCache.getStudyCount() + "</td></tr>"
			+ "<tr><td width=\"20%\">Studies complete:</td>"
			+ "<td>" + studyCache.getCompleteStudyCount() + "</td></tr>";
		return super.getStatusHTML(stageUniqueStatus);
	}

	class MonitorThread extends Thread {
		public MonitorThread() {
			super(servletContext + "-monitor");
		}
		public void run() {
			while (!stop) {
				studyCache.checkOpenStudies(System.currentTimeMillis() - minAge);
				if (deleteOnTransmission) {
					//Keep transmitted studies for 1 hour, just so the user can see that they went
					studyCache.deleteTransmittedStudies(System.currentTimeMillis() - 60 * 60 * 1000);
				}
				if (autosend && (firstDestinationKey != null)) {
					studyCache.sendCompleteStudies(firstDestinationKey);
				}
				try { Thread.sleep(minAge); }
				catch (Exception ignore) { }
			}
		}
	}

}
