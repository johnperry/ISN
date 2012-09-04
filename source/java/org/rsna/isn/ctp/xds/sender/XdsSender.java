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
package org.rsna.isn.ctp.xds.sender;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.rsna.ctp.pipeline.Status;
import org.rsna.isn.ctp.xds.sender.dicom.DicomStudy;
import org.rsna.isn.ctp.xds.sender.dicom.KosGenerator;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;
import org.rsna.isn.ctp.xds.sender.ihe.Iti41;
import org.rsna.isn.ctp.xds.sender.ihe.Iti8;
import org.w3c.dom.Element;

/**
 * XdsSender for RSNA image sharing network.
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 */
public class XdsSender
{
	private static final Logger logger = Logger.getLogger(XdsSender.class);

	private final List<XdsSubmissionListener> listenerList =
			Collections.synchronizedList(new ArrayList());

	private final URI iti8Pix;

	private final URI iti8Reg;

	private final URI iti41;

	private final String iti41SrcId;

	private final long timeout;

	/**
	 * Create an XdsSender instance
	 *
	 * @param element The DOM element containing the XdsSender configuration
	 *
	 * @throws URISyntaxException If there was an error reading from the
	 * configuration
	 */
	public XdsSender(Element element) throws URISyntaxException
	{
		this.iti8Pix = new URI(element.getAttribute("iti8Pix"));
		this.iti8Reg = new URI(element.getAttribute("iti8Reg"));
		this.iti41 = new URI(element.getAttribute("iti41"));
		this.iti41SrcId = element.getAttribute("iti41SrcId");
		this.timeout = NumberUtils.toLong(element.getAttribute("timeout"), 1000);

		logger.info("XdsSender instantiated");
	}

	/**
	 * Add a listener to be notified of events during the submission process
	 *
	 * @param listener The listener to be notified. Must not be null.
	 */
	public void addXDSSubmissionListener(XdsSubmissionListener listener)
	{
		//add the listener to the listenerList
		synchronized (listenerList)
		{
			listenerList.add(listener);
		}
	}

	/**
	 * Perform the XDS submission to the clearinghouse.
	 *
	 * @param files A list of DICOM part 10 files to be submitted.
	 * @param hash The hash to associate with the submission.
	 * @return The status of the submission request.
	 */
	public Status submit(List<File> files, String hash)
	{
		logger.info("submit request for "+files.size()+" files. Key = "+hash);

		Collection<DicomStudy> studies;


		try
		{
			studies = KosGenerator.processFiles(files, listenerList);

			logger.info("KosGenerator completed processing the files");
		}
		catch (Throwable ex)
		{
			logger.warn("Failed to generate KOS.", ex);

			return Status.FAIL;
		}


		try
		{
			Iti8.registerPatient(hash, iti8Pix, iti8Reg, listenerList);
		}
		catch (Throwable ex)
		{
			logger.warn("Failed to register patient.", ex);

			return Status.RETRY;
		}


		int currentIndex = 0;
		int total = files.size();
		for (DicomStudy study : studies)
		{
			try
			{
				logger.info("submitting the documents");

				currentIndex += Iti41.submitDocuments(study, hash, iti41, iti41SrcId,
						timeout, listenerList, currentIndex, total);

				logger.info("finished submitting the documents");
			}
			catch (Throwable ex)
			{
				logger.warn("Failed to submit objects for study: ", ex);

				return Status.RETRY;
			}
		}

		return Status.OK;
	}

}
