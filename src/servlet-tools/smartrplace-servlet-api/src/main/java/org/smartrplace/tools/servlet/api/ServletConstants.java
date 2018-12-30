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
package org.smartrplace.tools.servlet.api;

import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class ServletConstants {

	public static final String CONTEXT_NAME = "org.smartrplace.tools.rest.context";
	public static final String CONTEXT_FILTER = 
			"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")";
	public static final String DEFAULT_PATH_PREFIX = "/org/smartrplace";
	
	public static final String APP_AUTH_SERVLET = "/org/smartrplace/tools/app/auth";
	
}