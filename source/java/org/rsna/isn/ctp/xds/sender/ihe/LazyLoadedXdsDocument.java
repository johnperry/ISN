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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.openhealthtools.ihe.xds.document.DocumentDescriptor;
import org.openhealthtools.ihe.xds.document.XDSDocument;
import org.rsna.isn.ctp.xds.sender.event.Iti41Event;
import org.rsna.isn.ctp.xds.sender.event.XdsSubmissionListener;

/**
 * This class loads the contents of an XDS document on demand instead of
 * when an instance is instantiated.
 *
 * @author Wyatt Tellis
 * @version 3.0.0
 *
 */
class LazyLoadedXdsDocument extends XDSDocument
{
	private final List<XdsSubmissionListener> listeners;
	
	private final Iti41Event event;

    LazyLoadedXdsDocument(DocumentDescriptor descriptor, 
			File file, List<XdsSubmissionListener> listeners, 
			int index, int total)
    {
        super(descriptor);

        this.file = file;
		
		this.listeners = listeners;
		
		this.event = new Iti41Event(file, index + 1, total);
    }

    private final File file;

    /**
     * Get the value of file
     *
     * @return the value of file
     */
    public File getFile()
    {
        return file;
    }

    @Override
    public InputStream getStream()
    {
		synchronized (listeners)
		{
			for (XdsSubmissionListener listener : listeners)
			{
				listener.eventOccurred(event);
			}
		}
		
        return new AutoCloseInputStream(new LazyOpenFileInputStream(file));
    }

    private class LazyOpenFileInputStream extends InputStream
    {

        private final File file;

        private FileInputStream in = null;

        public LazyOpenFileInputStream(File file)
        {
            this.file = file;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            return in.read(b, off, len);
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            return in.read(b);
        }

        @Override
        public int read() throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            return in.read();
        }

        @Override
        public int available() throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            return in.available();
        }

        @Override
        public boolean markSupported()
        {
            try
            {
                if (in == null)
                {
                    in = new FileInputStream(file);
                }

                return in.markSupported();
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public synchronized void mark(int readlimit)
        {
            try
            {
                if (in == null)
                {
                    in = new FileInputStream(file);
                }

                in.mark(readlimit);
            }
            catch (IOException ex)
            {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public synchronized void reset() throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            in.reset();
        }

        @Override
        public long skip(long n) throws IOException
        {
            if (in == null)
            {
                in = new FileInputStream(file);
            }

            return in.skip(n);
        }

        @Override
        public void close() throws IOException
        {
            if (in != null)
                in.close();
        }

    }
}
