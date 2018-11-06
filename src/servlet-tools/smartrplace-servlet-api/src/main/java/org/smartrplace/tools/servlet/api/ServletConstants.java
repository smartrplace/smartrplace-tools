package org.smartrplace.tools.servlet.api;

import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ServletConstants {

	public static final String CONTEXT_NAME = "org.smartrplace.tools.rest.context";
	public static final String CONTEXT_FILTER = 
			"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")";
	public static final String DEFAULT_PATH_PREFIX = "/org/smartrplace";
	
	public static final String APP_AUTH_SERVLET = "/org/smartrplace/tools/app/auth";
	
}