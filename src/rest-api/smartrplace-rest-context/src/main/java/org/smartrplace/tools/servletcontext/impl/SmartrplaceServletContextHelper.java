package org.smartrplace.tools.servletcontext.impl;

import java.io.IOException;
import java.security.AccessControlContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ogema.accesscontrol.PermissionManager;
import org.ogema.accesscontrol.RestAccess;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.smartrplace.tools.rest.api.ServletAccessControl;
import org.smartrplace.tools.rest.api.ServletConstants;

@Component(
	service = {ServletContextHelper.class, ServletAccessControl.class}, 
	property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ServletConstants.CONTEXT_NAME,
		HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + ServletConstants.DEFAULT_PATH_PREFIX
	}
)
public class SmartrplaceServletContextHelper extends ServletContextHelper implements ServletAccessControl {
	
	@Reference
	private ComponentServiceObjects<PermissionManager> permManService;
    @Reference
    private ComponentServiceObjects<RestAccess> restAccessService;
	private final ThreadLocal<AccessControlContext> localContext = new ThreadLocal<AccessControlContext>();
	
    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	final RestAccess restAcc = restAccessService.getService();
    	try {
    		final AccessControlContext ctx = restAcc.getAccessContext(request, response);
    		if (ctx == null) {
    			request.removeAttribute(REMOTE_USER);
    			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    			return false;
    		}
    		if (!setUsername(request)) {
    			request.removeAttribute(REMOTE_USER);
    			response.sendError(HttpServletResponse.SC_FORBIDDEN);
    			return false;
    		}
    		setAccessControlContext(ctx);
    		return true;
    	} catch (ServletException e) {
    		request.removeAttribute(REMOTE_USER);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return false;
		} finally {
    		restAccessService.ungetService(restAcc);
    	}
    }
    
    private boolean setUsername(final HttpServletRequest request) {
    	final PermissionManager permMan = permManService.getService();
    	try {
    		final String user = permMan.getAccessManager().getCurrentUser();
    		if (user == null)
    			return false;
    		request.setAttribute(REMOTE_USER, user);
    		return true;
    	} finally {
    		permManService.ungetService(permMan);
    	}
    }

	private void setAccessControlContext(AccessControlContext ctx) {
		localContext.set(ctx);
	}

	@Override
	public AccessControlContext getAccessControlContext() {
		return localContext.get();
	}
	
}
