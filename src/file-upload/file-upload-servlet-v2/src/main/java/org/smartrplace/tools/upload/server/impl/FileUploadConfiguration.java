package org.smartrplace.tools.upload.server.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.smartrplace.tools.upload.server.FileUploadConstants;

@ObjectClassDefinition
public @interface FileUploadConfiguration {
	
	/**
	 * Folder (absolute or relative to rundir) 
	 * where files will be stored.
	 * Default is "data/uploads".
	 * @return
	 */
	String uploadFolder() default FileUploadConstants.DEFAULT_UPLOAD_FOLDER;
	
	/**
	 * Allowed maximum folder depth (including only the path info provided by 
	 * the client)
	 * Default 2.
	 * @return
	 */
	int maxDepth() default 2;
	
	/**
	 * Allowed maximum path length (does not include the file name)
	 * @return
	 */
	int maxPathLength() default 40;

	/**
	 * Allowed maximum file name length.
	 * @return
	 */
	int maxFileNameLength() default 25;

	/**
	 * The maximum allowed file size
	 * Default 100 MB
	 * @return
	 */
	long maxFileSize() default 1024*1024*100;
	
	/**
	 * The maximum allowed total request size
	 * Default 100 MB
	 * @return
	 */
	long maxRequestSize() default 1024*1024*100;
	
	/**
	 * The size threshold after which temp files will be written to disk.
	 * Default 5 MB
	 * @return
	 */
	int fileSizeThreshold() default 1024*1024*5;
	
	/**
	 * Each folder with uploaded files contains one config file which stores
	 * the {@link FileConfiguration} for all files in that folder.<br>
	 * Note: this property should not be changed after the initial framework start,
	 * otherwise the service will not be able to identify the old config files any more,
	 * in which case they will be ignored by the housekeeping.
	 * @return
	 */
	String configFileName() default "uploadConfig.json";
	
}
