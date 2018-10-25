package org.smartrplace.tools.upload.server.impl;

import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.smartrplace.tools.upload.server.FileUploadConstants;

/**
 * All properties in this type are optional, so it suffices to
 * create a configuration without any values, for the correct PID
 */
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
	int maxPathLength() default 50;

	/**
	 * Allowed maximum file name length.
	 * @return
	 */
	int maxFileNameLength() default 50;

	/**
	 * Use HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE instead: 
	 * "osgi.http.whiteboard.servlet.multipart.maxFileSize"
	 * The maximum allowed file size
	 * Default 100 MB
	 * @return
	 */
//	long maxFileSize() default 1024*1024*100;
	
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
	 * Maximum number of uploads per user per minute. Default: 4.
	 * @return
	 */
	int maxUploadsPerMinute() default 4;
	
	/**
	 * Maximum number of uploads per user per hour. Default: 10.
	 * @return
	 */
	int maxUploadsPerHour() default 10;
	
	/**
	 * Maximum number of uploads per user per day. Default: 15.
	 * @return
	 */
	int maxUploadsPerDay() default 15;
	
	/**
	 * Each folder with uploaded files contains one config file which stores
	 * the {@link FileConfiguration} for all files in that folder.<br>
	 * Note: this property should not be changed after the initial framework start,
	 * otherwise the service will not be able to identify the old config files any more,
	 * in which case they will be ignored by the housekeeping.
	 * @return
	 */
	String configFileName() default FileUploadConstants.DEFAULT_CONFIG_FILE;
	
}
