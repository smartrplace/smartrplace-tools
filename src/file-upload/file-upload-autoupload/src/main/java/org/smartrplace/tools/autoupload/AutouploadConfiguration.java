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
package org.smartrplace.tools.autoupload;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Note: this also requires properties
 * <ul>
 *   <li>org.smartrplace.tools.housekeeping.Period</li>
 *   <li>org.smartrplace.tools.housekeeping.Delay</li>
 *   <li>org.smartrplace.tools.housekeeping.Unit</li>
 * </ul>
 */
@ObjectClassDefinition
public @interface AutouploadConfiguration {
	
	public static final String FACTORY_PID = "org.smartrplace.tools.Autoupload";

	String remoteUrl();

	/**
	 * Path relative to rundir for file to be uploaded
	 * @return
	 */
	String localPath();
	
	/**
	 * Path info
	 * @return
	 */
	String remotePath() default "";
	
	boolean incrementalUpload() default false;
	
	/**
	 * File will be deleted after the specified number of days.
	 * Set to zero or a negative value to retain file forever.
	 * @return
	 */
	long daysToKeepFile() default -1;
	
	/**
	 * Maximum number of files (of the same name) to store.
	 * Set to zero or a negative value to ignore the total number
	 * of files. If {@link #overwrite} is true this is ignored.
	 * @return
	 */
	int maxFilesToKeep() default 25;
	
	/**
	 * This refers to the maximum size in bytes to store (sum of all versions
	 * of one file). When the size is exceeded, some older files will be
	 * deleted. The default value is 50MB.
	 * @return
	 */
	long maxSize() default 50 * 1024 * 1024;
	
	/**
	 * Date time format or empty string for millis (default)
	 * @return
	 */
	String incrementalUploadDateTimeFormat() default "";
	
	String incrementalUploadFilePrefix() default "";
	
	long timeout() default 20;
	
	String timeoutUnit() default "SECONDS"; 
	
}
