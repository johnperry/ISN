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
import org.rsna.server.HttpServer;
import org.rsna.server.ServletSelector;
import org.rsna.util.StringUtil;
import org.w3c.dom.Element;

/**
 * An ExportService that caches studies and sends them to the ISN Clearinghouse.
 */
public class CachingXDSExportService extends AbstractPipelineStage implements ExportService {

	static final Logger logger = Logger.getLogger(CachingXDSExportService.class);

	XDSStudyCache cache = null;
	String servletContext = "";
	long minAge = 300;
	int count = 0;
	long timeDepth = 0;
	String objectCacheID = "";
	ObjectCache objectCache = null;
	boolean deleteOnTransmission = true;
	MonitorThread monitor = null;

	/**
	 * Construct a CachingXDSExportService.
	 * @param element the XML element from the configuration file,
	 * specifying the configuration of the stage.
	 */
	public CachingXDSExportService(Element element) throws Exception {
		super(element);
		if (root == null) logger.error(name+": No root directory was specified.");
		else root.mkdirs();
		servletContext = element.getAttribute("servletContext").trim();
		if (servletContext.equals("")) servletContext = "XDSSender";
		minAge = Math.max( StringUtil.getLong(element.getAttribute("minAge")), minAge ) * 1000;
		timeDepth = StringUtil.getLong(element.getAttribute("timeDepth"));
		String objectCacheID = element.getAttribute("cacheID").trim();
		cache = XDSStudyCache.getInstance(servletContext, root);

		deleteOnTransmission = !element.getAttribute("deleteOnTransmission").equals("no");
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

		//Install the servlet on the context
		HttpServer server = config.getServer();
		ServletSelector selector = server.getServletSelector();
		selector.addServlet(servletContext, XDSSenderServlet.class);

		//Create and start the monitor thread
		monitor = new MonitorThread();
		monitor.start();
	}

	/**
	 * Stop the pipeline stage.
	 */
	public void shutdown() {
		stop = true;
		cache.close();
	}

	/**
	 * Determine whether the pipeline stage has shut down.
	 */
	public boolean isDown() {
		return cache.isClosed();
	}

	/**
	 * Add a FileObject to the cache.
	 */
	public synchronized void export(FileObject fileObject) {
		count++;
		FileObject phiObject = (objectCache != null) ? objectCache.getCachedObject() : null;
		cache.store(fileObject, phiObject);
	}

	/**
	 * Get HTML text displaying the active status of the stage.
	 * @return HTML text displaying the active status of the stage.
	 */
	public String getStatusHTML() {
		String stageUniqueStatus =
			"<tr><td width=\"20%\">Files received:</td><td>" + count + "</td></tr>"
			+ "<tr><td width=\"20%\">Studies cached:</td>"
			+ "<td>" + cache.getStudyCount() + "</td></tr>"
			+ "<tr><td width=\"20%\">Studies complete:</td>"
			+ "<td>" + cache.getCompleteStudyCount() + "</td></tr>";
		return super.getStatusHTML(stageUniqueStatus);
	}

	class MonitorThread extends Thread {
		public MonitorThread() {
			super(servletContext + "-monitor");
		}
		public void run() {
			while (!stop) {
				cache.checkOpenStudies(System.currentTimeMillis() - minAge);
				if (deleteOnTransmission) {
					//Keep transmitted studies for 1 hour, just so the user can see that they went
					cache.deleteTransmittedStudies(System.currentTimeMillis() - 60 * 60 * 1000);
				}
				try { Thread.sleep(minAge); }
				catch (Exception ignore) { }
			}
		}
	}

}
