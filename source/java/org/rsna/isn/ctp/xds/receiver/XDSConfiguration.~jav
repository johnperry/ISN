/* Copyright (c) <2011>, <Radiological Society of North America>
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

package org.rsna.isn.ctp.xds.receiver;

import java.io.File;
import org.w3c.dom.Element;

/**
 * Get values of configuration from Element
 * which can be used by downstream classes
 */
public class XDSConfiguration {
    public String registryURL;
    public String repositoryURL;
    public String rad69URL;
    public String repositoryUniqueID;
    public String assigningAuthorityUniversalID;
    public String assigningAuthorityUniversalIDType;
    public String homeCommunityID;
    public int imagesPerRequest;

    static XDSConfiguration configuration = null;
    Element element;

    public static synchronized XDSConfiguration getInstance() {
        return configuration;
    }

    public static XDSConfiguration load (Element element) {
        configuration = new XDSConfiguration(element);
        return configuration;
    }

    protected XDSConfiguration(Element element) {
        this.element = element;
        rad69URL = element.getAttribute("rad69URL");
		registryURL = element.getAttribute("registryURL");
        repositoryURL = element.getAttribute("repositoryURL");

        repositoryUniqueID = element.getAttribute("repositoryUniqueID");
        assigningAuthorityUniversalID = element.getAttribute("assigningAuthorityUniversalID");
        assigningAuthorityUniversalIDType = element.getAttribute("assigningAuthorityUniversalIDType");
        homeCommunityID = element.getAttribute("homeCommunityID");
        imagesPerRequest= Integer.parseInt(element.getAttribute("imagesPerRequest"));
    }
}
