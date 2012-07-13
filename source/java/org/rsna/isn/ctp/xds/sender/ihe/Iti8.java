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
package org.rsna.isn.ctp.xds.sender.ihe;

import java.net.URI;
import java.util.List;
import org.apache.log4j.Logger;
import org.eclipse.ohf.hl7v2.core.message.MessageManager;
import org.openhealthtools.ihe.atna.auditor.PIXSourceAuditor;
import org.openhealthtools.ihe.common.mllp.MLLPDestination;
import org.openhealthtools.ihe.pix.source.PixMsgRegisterOutpatient;
import org.openhealthtools.ihe.pix.source.PixSource;
import org.openhealthtools.ihe.pix.source.PixSourceResponse;
import org.openhealthtools.ihe.utils.IHEException;
import org.rsna.isn.ctp.xds.sender.event.Iti8Event;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;

/**
 * This class implements the ITI-8 (Patient identity feed) transaction.
 *
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 */
public class Iti8
{
	private static final Logger logger = Logger.getLogger(Iti8.class);

	private static final MessageManager manager = MessageManager.getFactory();

	static
	{
		PIXSourceAuditor.getAuditor().getConfig().setAuditorEnabled(false);
	}
	
	private Iti8()
	{
	}

	/**
	 * Register the patient with the PIX and registry.
	 *
	 * @param hash The hash to assign to the submission.
	 * @param pix The URI for the PIX
	 * @param registry The URI for the registry
	 * @param listeners  A list of listeners to be notified of events during the
	 * ITI-8 transaction.
	 * @throws IHEException If there was an uncaught exception while attempting
	 * to register the patient.
	 * @throws ClearinghouseException If the PIX or registry returned an error 
	 */
	public static void registerPatient(String hash, URI pix, URI registry, List<XdsSubmissionListener> listeners)
			throws IHEException, ClearinghouseException
	{
		sendIti8Message(hash, pix, "PIX", listeners);

		sendIti8Message(hash, registry, "registry", listeners);
	}

	private static void sendIti8Message(String hash, URI uri, String remoteType, List<XdsSubmissionListener> listeners)
			throws IHEException, ClearinghouseException
	{
		synchronized (listeners)
		{
			Iti8Event event = new Iti8Event(remoteType);

			for (XdsSubmissionListener listener : listeners)
			{
				listener.eventOccurred(event);
			}
		}

		PixSource feed = new PixSource();

		MLLPDestination mllp = new MLLPDestination(uri);
		MLLPDestination.setUseATNA(false);
		feed.setMLLPDestination(mllp);


		PixMsgRegisterOutpatient msg = new PixMsgRegisterOutpatient(manager,
				null, hash,
				null, Constants.RSNA_UNIVERSAL_ID,
				Constants.RSNA_UNIVERSAL_ID_TYPE);
		msg.addOptionalPatientNameFamilyName("RSNA ISN");
		msg.addOptionalPatientNameGivenName("RSNA ISN");


		PixSourceResponse rsp = feed.sendRegistration(msg, false);

		String code = rsp.getResponseAckCode(false);
		String error = rsp.getField("MSA-3");

		if ("AE".equals(code))
		{
			String chMsg = "Clearinghouse " + remoteType
					+ " failed to process ITI-8 message.  Error returned was: " + error;

			throw new ClearinghouseException(chMsg);
		}
		else if ("AR".equals(code))
		{

			if (error.startsWith("PIX-10000:"))
			{
				logger.info("Clearinghouse " + remoteType
						+ " reports patient id " + hash
						+ " has already been registered.");
			}
			else
			{
				String chMsg = "Clearinghouse " + remoteType
						+ " rejected ITI-8 message. Error returned was: " + error;

				throw new ClearinghouseException(chMsg);
			}
		}
	}

}
