package org.smartrplace.tools.profiles.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
public @interface ProfileGeneratorConfiguration {
	
	/**
	 * PID of the default implementation
	 * @return
	 */
	public static final String PID = "org.smartrplace.tools.profiles.Generator";

	@AttributeDefinition(
			description="If this is empty (the default), then the bundle persistent storage area will be used to store files. Otherwise the specified path.")
	String storagePath() default "";
	
}
