package org.smartrplace.tools.upload.tests;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.context.ServletContextHelper;

/*
@Component(
		scope=ServiceScope.SINGLETON,
		service=ServletContextHelper.class, property= {
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + FileUploadConstants.CONTEXT_NAME,
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + FileUploadConstants.PATH
		}
	)
*/
public class TestContextHelper extends ServletContextHelper {
	
	public static final String TEST_USER = "testUser";
	
	// just for testing
	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setAttribute(ServletContextHelper.REMOTE_USER, TEST_USER);
		return true;
	}

}
