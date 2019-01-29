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
package org.smartrplace.apps.humidity.warning.model;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.Configuration;

public interface WarningConfiguration extends Configuration {

	/**
	 * A reference
	 * @return
	 */
	Room room();
	
	FloatResource upperThresholdHumidity();
	
	FloatResource lowerThresholdHumidity();
	
	TimeResource upperTimeout();
	
	TimeResource lowerTimeout();
	
	TimeResource lastWarningLowHumidity();
	
	TimeResource lastWarningHighHumidity();
	
	TimeResource minWarningInterval();
	
	/**
	 * If false, a warning is generated when a single sensor
	 * exceeds the threshold, otherwise the average value of 
	 * all sensors in a room is used. Default: false
	 * @return
	 */
	BooleanResource useRoomAverage();
	
	/**
	 * State
	 */
	BooleanResource high();
	
	/**
	 * State
	 */
	BooleanResource low();	
	
	default void setState(final BooleanResource res, boolean state) {
		if (!state && !res.isActive())
			return;
		if (state)
			res.<BooleanResource> create();
		res.setValue(state);
		res.activate(false);
	}
	
	default void setHigh(boolean high) {
		setState(high(), high);
	}
	
	default void setLow(boolean low) {
		setState(low(), low);
	}
	
	default boolean isHigh() {
		return high().isActive() && high().getValue();
	}
	
	default boolean isLow() {
		return low().isActive() && low().getValue();
	}
	
	
}

