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
package org.smartrplace.tools.profiles.prefs.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.prototypes.Configuration;
import org.smartrplace.tools.profiles.ProfileTemplate;

public interface ProfileConfiguration extends Configuration {

	/**
	 * The {@link ProfileTemplate#id() id} of a {@link ProfileTemplate}. 
	 * @return
	 */
	StringResource profileTemplateId();
	
	/**
	 * Must be unique per {@link #profileTemplateId()}
	 * @return
	 */
	StringResource id();
	
	ResourceList<DataPointConfig> dataPoints();
	
	OnOffSwitch onOffSwitch();
	
	StringResource endState();
	
	/**
	 * Order of durations must correspond to the order of states in
	 * {@link ProfileTemplate#states()}.
	 * @return
	 */
	ResourceList<StateDuration> durations();
	
}
