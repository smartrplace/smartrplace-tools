package org.smartrplace.tools.servlet.api;

import java.security.AccessControlContext;

public interface AppAuthentication {

	AccessControlContext getContext(char[] token);

}
