/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.servlet.api.AppAuthentication;
import org.smartrplace.tools.servlet.api.ServletAccessControl;
import org.smartrplace.tools.servlet.api.ServletConstants;

@Component(
	service = {ServletContextHelper.class, ServletAccessControl.class}, 
	property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ServletConstants.CONTEXT_NAME,
		HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH + "=" + ServletConstants.DEFAULT_PATH_PREFIX
	}
)
public class SmartrplaceServletContextHelper extends ServletContextHelper implements ServletAccessControl {
	
	@Reference(service=PermissionManager.class)
	private ComponentServiceObjects<PermissionManager> permManService;
    @Reference(service=AppAuthentication.class)
    private ComponentServiceObjects<AppAuthentication> appAuthService;
    @Reference(service=RestAccess.class)
    private ComponentServiceObjects<RestAccess> restAccService;    
	private final ThreadLocal<AccessControlContext> localContext = new ThreadLocal<AccessControlContext>();
	
    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	try {
    		final AccessControlContext ctx = getContext(request);
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
			LoggerFactory.getLogger(getClass()).warn("Servlet exception",e);
			return false;
    	}
    }
    
    private AccessControlContext getContext(final HttpServletRequest req) throws ServletException, IOException {
    	final RestAccess ra = restAccService.getService();
    	try {
    		final AccessControlContext ctx = ra.getAccessContext(req, null);
    		if (ctx != null)
    			return ctx;
    	} catch (NullPointerException expected) { 
    	} finally {
    		restAccService.ungetService(ra);
    	}
    	final String token = req.getHeader("Authorization");
    	if (token == null || !token.toLowerCase().startsWith("bearer "))
    		return null;
    	final String token1 = token.substring("bearer ".length());
    	final AppAuthentication appAuth = appAuthService.getService();
    	try {
    		return appAuth.getContext(token1.toCharArray());
    	} finally {
    		appAuthService.ungetService(appAuth);
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
