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
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.tools.resource.util.ResourceUtils;

public class TemperaturePattern extends ResourcePattern<TemperatureSensor> {

	public TemperaturePattern(Resource match) {
		super(match);
	}

	public final TemperatureResource reading = model.reading();
	
	public final Room room = ResourceUtils.getDeviceLocationRoom(model);
	
}
