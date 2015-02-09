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

import java.security.MessageDigest;
import java.math.BigInteger;
import org.rsna.util.StringUtil;
 
public class TransHash {
    
	public static String gen(String userEmail, String dateOfBirth, String accessCode) throws Exception {
		
		dateOfBirth = fixDate(dateOfBirth);

		MessageDigest md = MessageDigest.getInstance("SHA-256");

		md.update(userEmail.toLowerCase().getBytes("UTF8"));
		md.update(dateOfBirth.getBytes("UTF8"));
		md.update(accessCode.toLowerCase().replaceAll("[^ybndrfg8ejkmcpqxot1uwisza345h769]","").getBytes("UTF8"));

		return String.format("%064x", (new BigInteger(1, md.digest())));
	}

	public static String fixDate(String date) {
		int y, m, d;
		int x0, x1, x2;
		
		String[] x = date.split("[/\\.]");
		
		if (x.length == 3) {
			x0 = StringUtil.getInt(x[0]);
			x1 = StringUtil.getInt(x[1]);
			x2 = StringUtil.getInt(x[2]);
			
			if (x0 > 1000) {
				m = x1;
				d = x2;
				y = x0;
			}
			else {
				if (x0 <= 12) {
					m = x0;
					d = x1;
					y = x2;
				}
				else {
					m = x0;
					d = x1;
					y = x2;
				}					
			}
			date = String.format( "%04d%02d%02d", y, m, d);
		}
		return date;
	}
}

