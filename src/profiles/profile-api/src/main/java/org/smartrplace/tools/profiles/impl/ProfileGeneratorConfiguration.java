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
