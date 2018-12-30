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
package org.smartrplace.tools.profiles.prefs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.ogema.core.model.Resource;
import org.ogema.model.actors.OnOffSwitch;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;

public interface ProfilePreferences {
	
	/**
	 * @param template
	 * 		the profile template the configuration applies to
	 * @param id
	 * 		a unique (per template) id; if the id is already in use, the configuration
	 * 		is replaced
	 * @param resourceSettings
	 * 		the resource values
	 */
	default Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings) {
		return storeProfileConfiguration(template, id, resourceSettings, null, null, null);
	}
	
	/**
	 * 	
	 * @param template
	 * 		the profile template the configuration applies to
	 * @param id
	 * 		a unique (per template) id; if the id is already in use, the configuration
	 * 		is replaced
	 * @param resourceSettings
	 * 		the resource values
	 * @param durations
	 * 		must be either null or of the same size as {@link ProfileTemplate#states() template#states}, 
	 * 		with states in the same order
	 * @param endState
	 * 		may be null
	 * @param onOffSwitch
	 * 		may be null
	 * @return
	 * @throws IllegalArgumentException if durations is not null and its size does not match the size 
	 * 		of {@link ProfileTemplate#states() template#states}.
	 */
	Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings,
			List<StateDuration> durations,
			State endState,
			OnOffSwitch onOffSwitch);
	
	Future<ProfileData> loadProfileConfiguration(
			ProfileTemplate template,
			String id);
	
	Future<Collection<String>> getProfileIds(String templateId);
	
}
