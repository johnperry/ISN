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
package org.rsna.isn.ctp.xds.sender.event;

import java.io.File;

/**
 * Class for representing events from document submission process (ITI-41)
 * 
 * @author Wyatt Tellis
 * @version 3.0.0
 * 
 */
public class Iti41Event implements XdsSubmissionEvent
{

	/**
	 * Create an instance of an Iti41Event. 
	 * 
	 * @param file The file associated with this event. 
	 * @param currentImage The one based index of the file within the list of files
	 * being processed. 
	 * @param totalImages The total number of file being processed. 
	 */
	public Iti41Event(File file, int currentImage, int totalImages)
	{
		this.file = file;
		this.currentImage = currentImage;
		this.totalImages = totalImages;
	}
	
	
	private final File file;

	/**
	 * Get the current file being processed
	 *
	 * @return A file instance
	 */
	public File getFile()
	{
		return file;
	}

	private final int currentImage;

	/**
	 * Get the one based index of the image currently being processed
	 *
	 * @return the current index
	 */
	public int getCurrentImage()
	{
		return currentImage;
	}

	private final int totalImages;

	/**
	 * Get the total number of images being processed.
	 *
	 * @return the image total
	 */
	public int getTotalImages()
	{
		return totalImages;
	}

	@Override
	public String toString()
	{
		return "Submitting documents. "
				+ "Sent object " + currentImage + " of " + totalImages + ". "
				+ "File path is: " + file;
	}

	
}
