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
package org.smartrplace.tools.upload.api;

/**
 * Housekeeping configuration for a single file.
 * This configuration determines how a single file 
 * (i.e. all uploaded files with the same name, by a single user)
 * is treated: whether the file is overwritten every time a new version
 * is uploaded, and if not how long different versions are kept.
 */
// FIXME builder pattern?
public class FileConfiguration {

	/** 
	 * The files (in the same folder) this refers to.
	 * Full filenames will be of the form {@link #filePrefix} + "_" + yyyyMMddHHmm + "." + {@link #fileEnding}
	 */
	public String filePrefix;
	
	/** 
	 * Full filenames will be of the form {@link #filePrefix} + "_" + yyyyMMddHHmm + "." + {@link #fileEnding}.
	 * May be null.
	 */
	public String fileEnding;
	
	/**
	 * Overwrite file when a new file with the same name 
	 * is submitted? 
	 */
	public boolean overwrite = true;
	
	/**
	 * File will be deleted after the specified number of days.
	 * Set to zero or a negative value to retain file forever.
	 */
	public long daysToKeepFile = -1; 

	/**
	 * Maximum number of files (of the same name) to store.
	 * Set to zero or a negative value to ignore the total number
	 * of files. If {@link #overwrite} is true this is ignored.
	 */
	public int maxFilesToKeep = 25;
	
	/**
	 * This refers to the maximum size in bytes to store (sum of all versions
	 * of one file). When the size is exceeded, some older files will be
	 * deleted. The default value is 50MB.
	 */
	public long maxSize = 50 * 1024 * 1024;
	
	/**
	 * Only relevant if {@link #overwrite} is false and either {@link #maxFilesToKeep} 
	 * is positive or {@link #maxSize} is positive. Determines which files will be deleted first when the maximum 
	 * number of versions of a single file is exceeded, or the total size of all versions of a file
	 * exceeds the configured maximum. <br>
	 * If true, then the housekeeping
	 * will simply delete the eldest versions of a file. If false (the default), some elder versions 
	 * will be retained along with some newer ones (the last two versions are always retained).
	 */
	public boolean deleteOldFilesFirst = false; // TODO implement case true
		
}
