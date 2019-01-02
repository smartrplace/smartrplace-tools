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
