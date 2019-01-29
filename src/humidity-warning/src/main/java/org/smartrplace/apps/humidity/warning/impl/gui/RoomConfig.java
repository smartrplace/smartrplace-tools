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
package org.smartrplace.apps.humidity.warning.impl.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.Validate;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.humidity.warning.impl.Utils;
import org.smartrplace.apps.humidity.warning.impl.pattern.HumidityPattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.TemperaturePattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.WarningConfigurationPattern;

class RoomConfig {

	final Room room;
	final List<HumidityPattern> humiditySensors = new ArrayList<>(2);
	final List<TemperaturePattern> temperatureSensors = new ArrayList<>(2);
	// may be null
	volatile WarningConfigurationPattern warningConfig;
	private boolean finished = false;
	private float avgHumidity; // 0 - 1
	private float avgTemperatureCelsius;
	private float dewPointCelsius;
	
	RoomConfig(Room room) {
		Room r = room;
		while (r.isReference(true))
			r = r.getLocationResource();
		this.room = r;
	}
	
	void calcAverages() {
		if (temperatureSensors.isEmpty())  {
			avgTemperatureCelsius = Float.NaN;
		} else {
			avgTemperatureCelsius = (float) temperatureSensors.stream()
				.mapToDouble(pattern -> pattern.reading.getCelsius())
				.average().orElse(Float.NaN);
		}
		if (humiditySensors.isEmpty())
			avgHumidity = Float.NaN;
		else 
			avgHumidity = (float) humiditySensors.stream()
			.mapToDouble(pattern -> pattern.reading.getValue())
			.average().orElse(Float.NaN);
		if (!Float.isFinite(avgTemperatureCelsius) || !Float.isFinite(avgHumidity))
			dewPointCelsius = Float.NaN;
		else
			dewPointCelsius = Utils.calculateDewPoint(avgTemperatureCelsius, avgHumidity);
		finished = true;
	}
	
	private void checkState() {
		if (!finished)
			calcAverages();

	}
	
	float getAverageHumidity() {
		checkState();
		return avgHumidity;
	}
	
	float getAverageTemperatureCelsius() {
		checkState();
		return avgTemperatureCelsius;
	}
	
	float getDewPointCelsius() {
		checkState();
		return dewPointCelsius;
	}
	
	int getHumidityThreshold(final boolean highOrLow) {
		final WarningConfigurationPattern warningConfig = this.warningConfig;
		if (highOrLow) {
			if (warningConfig != null && warningConfig.upperThresholdHumidity.isActive())
				return (int) (warningConfig.upperThresholdHumidity.getValue() * 100);
			return 65;
		} else {
			if (warningConfig != null && warningConfig.lowerThresholdHumidity.isActive())
				return (int) (warningConfig.lowerThresholdHumidity.getValue() * 100);
			return 35;
		}
	}
	
	void setHumidityThreshold(final boolean highOrLow, final int newValue) {
		final WarningConfigurationPattern warningConfig = this.warningConfig;
		if (warningConfig == null || newValue < 0 || newValue > 100)
			return;
		if ((highOrLow && !warningConfig.upperThresholdHumidity.isActive()) ||
				(!highOrLow && !warningConfig.lowerThresholdHumidity.isActive()))
			return;
		if (highOrLow)
			warningConfig.upperThresholdHumidity.setValue(((float) newValue)/100); 
		else
			warningConfig.lowerThresholdHumidity.setValue(((float) newValue)/100); 
		
	}
	
	boolean thresholdViolated(boolean highOrLow) {
		final WarningConfigurationPattern warningConfig = this.warningConfig;
		if (warningConfig == null)
			return false;
		if ((highOrLow && !warningConfig.upperThresholdHumidity.isActive()) ||
				(!highOrLow && !warningConfig.lowerThresholdHumidity.isActive()))
			return false;
		final DoubleStream ds = humiditySensors.stream()
				.mapToDouble(sensor -> sensor.reading.getValue())
				.filter(val -> val >= 0 && val <= 1);
		final OptionalDouble opt  = highOrLow ? ds.max() : ds.min();
		if (!opt.isPresent())
			return false;
		return highOrLow ? (opt.getAsDouble() > warningConfig.upperThresholdHumidity.getValue()) :
			(opt.getAsDouble() < warningConfig.lowerThresholdHumidity.getValue());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RoomConfig))
			return false;
		return ((RoomConfig) obj).room.equalsLocation(this.room);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(room);
	}
	
	@Override
	public String toString() {
		return "RoomConfig[" + room.getLocation() + "]";
	}
	
}
