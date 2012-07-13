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
package org.rsna.isn.ctp.xds.sender.dicom;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is used to track the DICOM series that make up a submission
 * set.
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 */
public class DicomSeries
{
	private String seriesUid;

	/**
	 * Get the value of seriesUid
	 *
	 * @return the value of seriesUid
	 */
	public String getSeriesUid()
	{
		return seriesUid;
	}

	/**
	 * Set the value of seriesUid
	 *
	 * @param seriesUid new value of seriesUid
	 */
	public void setSeriesUid(String seriesUid)
	{
		this.seriesUid = seriesUid;
	}

	private String seriesDescription;

	/**
	 * Get the value of seriesDescription
	 *
	 * @return the value of seriesDescription
	 */
	public String getSeriesDescription()
	{
		return seriesDescription;
	}

	/**
	 * Set the value of seriesDescription
	 *
	 * @param seriesDescription new value of seriesDescription
	 */
	public void setSeriesDescription(String seriesDescription)
	{
		this.seriesDescription = seriesDescription;
	}

	private String modality;

	/**
	 * Get the value of modality
	 *
	 * @return the value of modality
	 */
	public String getModality()
	{
		return modality;
	}

	/**
	 * Set the value of modality
	 *
	 * @param modality new value of modality
	 */
	public void setModality(String modality)
	{
		this.modality = modality;
	}

	private Map<String, DicomObject> objects = new LinkedHashMap();

	/**
	 * Get the DICOM objects associated with this series.
	 *
	 * @return A set containing the associated DICOM objects.
	 */
	public Map<String, DicomObject> getObjects()
	{
		return objects;
	}

}
