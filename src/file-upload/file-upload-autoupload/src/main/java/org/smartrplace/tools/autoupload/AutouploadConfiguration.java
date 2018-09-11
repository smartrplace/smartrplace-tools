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
	 * Date time format or empty string for millis (default)
	 * @return
	 */
	String incrementalUploadDateTimeFormat() default "";
	
	String incrementalUploadFilePrefix() default "";
	
	long timeout() default 20;
	
	String timeoutUnit() default "SECONDS"; 
	
}
