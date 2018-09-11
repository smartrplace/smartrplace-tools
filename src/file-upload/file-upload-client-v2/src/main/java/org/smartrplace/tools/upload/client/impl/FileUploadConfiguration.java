package org.smartrplace.tools.upload.client.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface FileUploadConfiguration {

	String remoteUrl();
	
	String remoteUser();
	
	String remotePw();
	
	boolean disableHostnameVerification() default false;
	
	boolean disableHostCertVerification() default false;
	
	int getRequestTimeoutSeconds() default 30;
	
}
