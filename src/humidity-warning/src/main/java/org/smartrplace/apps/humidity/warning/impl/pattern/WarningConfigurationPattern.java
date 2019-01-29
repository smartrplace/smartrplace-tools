/**
 * ﻿Copyright 2019 Smartrplace UG
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
package org.smartrplace.apps.humidity.warning.impl.pattern;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.humidity.warning.model.WarningConfiguration;

public class WarningConfigurationPattern extends ResourcePattern<WarningConfiguration> {

	public WarningConfigurationPattern(Resource match) {
		super(match);
	}

	@ChangeListener(structureListener=true)
	public final Room room = model.room();
	
	/**
	 * Value between 0 and 1
	 */
	@Existence(required=CreateMode.OPTIONAL)
	public final FloatResource upperThresholdHumidity = model.upperThresholdHumidity();

	@Existence(required=CreateMode.OPTIONAL)
	public final FloatResource lowerThresholdHumidity = model.lowerThresholdHumidity();

	@Existence(required=CreateMode.OPTIONAL)
	public final TimeResource lastWarningLowHumidity = model.lastWarningLowHumidity();
	
	@Existence(required=CreateMode.OPTIONAL)
	public final TimeResource lastWarningHighHumidity = model.lastWarningHighHumidity();
	
	@Existence(required=CreateMode.OPTIONAL)
	public final TimeResource upperTimeout = model.upperTimeout();

	@Existence(required=CreateMode.OPTIONAL)
	public final TimeResource lowerTimeout = model.lowerTimeout();
	
}
