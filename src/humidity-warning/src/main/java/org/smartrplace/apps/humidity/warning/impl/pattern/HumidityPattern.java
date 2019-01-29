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
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.tools.resource.util.ResourceUtils;

public class HumidityPattern extends ResourcePattern<HumiditySensor> {

	public HumidityPattern(Resource match) {
		super(match);
	}
	
	public final FloatResource reading = model.reading();
	
	@ChangeListener(structureListener=true)
	public final Room room = ResourceUtils.getDeviceLocationRoom(model);

	@Existence(required=CreateMode.OPTIONAL)
	@ChangeListener(structureListener=true)
	private final Room room0 = model.location().room();

	@Existence(required=CreateMode.OPTIONAL)
	@ChangeListener(structureListener=true)
	private final Room parentRoom = getParentRoom(model);

	private static Room getParentRoom(final HumiditySensor sensor) {
		Resource r = sensor.getLocationResource().getParent();
		while (r != null) {
			if (r instanceof PhysicalElement)
				return ((PhysicalElement) r).location().room();
			r = r.getParent();
		}
		return null;
	}
			
			
			
}
