/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package org.rsna.isn.ctp;

import java.io.File;
import org.apache.log4j.Logger;
import org.rsna.server.User;
import org.rsna.server.Users;

public class ISNRoles {

	static final Logger logger = Logger.getLogger(ISNRoles.class);

	/**
	 * Add the ISN roles and make sure the admin user has them.
	 */
	public static void init() {
		Users users = Users.getInstance();
		if (users != null) {
			users.addRole("import");
			users.addRole("export");
			User admin = users.getUser("admin");
			if (admin != null) {
				admin.addRole("import");
				admin.addRole("export");
			}
		}
	}
}
