package org.smartrplace.tools.ssl.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
		description="Configuration for the SSL context service, in particular the paths "
				+ "to the keystore and truststore files."
)
public @interface SslConfig {
	
	String keystorePath() default "config/keystore";
	
	String truststorePath() default "config/truststore";
	
	/**
	 * If not set, the system default is used, typically "jks".
	 * @return
	 */
	String keystoreType() default "";

	/**
	 * If not set, a default algorithm will be used.
	 * @return
	 */
	String algorithm() default "";
	
	/**
	 * If not set, a default provider will be chosen.
	 * @return
	 */
	String provider() default "";
	
	String keystorePassword() default ""; 
	
}
