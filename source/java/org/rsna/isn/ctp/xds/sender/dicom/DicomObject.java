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

import java.io.File;

/**
 * This class is used to track the DICOM objects that make up a submission
 * set.
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 * 
 */
public class DicomObject
{
	private String sopInstanceUid;

	/**
	 * Get the value of sopInstanceUid
	 *
	 * @return the value of sopInstanceUid
	 */
	public String getSopInstanceUid()
	{
		return sopInstanceUid;
	}

	/**
	 * Set the value of sopInstanceUid
	 *
	 * @param sopInstanceUid new value of sopInstanceUid
	 */
	public void setSopInstanceUid(String sopInstanceUid)
	{
		this.sopInstanceUid = sopInstanceUid;
	}

	private String sopClassUid;

	/**
	 * Get the value of sopClassUid
	 *
	 * @return the value of sopClassUid
	 */
	public String getSopClassUid()
	{
		return sopClassUid;
	}

	/**
	 * Set the value of sopClassUid
	 *
	 * @param sopClassUid new value of sopClassUid
	 */
	public void setSopClassUid(String sopClassUid)
	{
		this.sopClassUid = sopClassUid;
	}

	private String transferSyntaxUid;

	/**
	 * Get the value of transferSyntaxUid
	 *
	 * @return the value of transferSyntaxUid
	 */
	public String getTransferSyntaxUid()
	{
		return transferSyntaxUid;
	}

	/**
	 * Set the value of transferSyntaxUid
	 *
	 * @param transferSyntaxUid new value of transferSyntaxUid
	 */
	public void setTransferSyntaxUid(String transferSyntaxUid)
	{
		this.transferSyntaxUid = transferSyntaxUid;
	}


	private File file;

	/**
	 * Get the file containing the DICOM object
	 *
	 * @return A file instance
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Set the value of file
	 *
	 * @param file A file instance
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

}
