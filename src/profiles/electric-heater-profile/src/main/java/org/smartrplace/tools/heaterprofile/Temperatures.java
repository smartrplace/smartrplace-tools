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
package org.smartrplace.tools.heaterprofile;

import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.tools.profiles.DataPoint.DataType;
import org.smartrplace.tools.profiles.utils.DataPointImpl;

public class Temperatures {

	public static DataPointImpl temperatureHeated 
		= new DataPointImpl("tempHeated", TemperatureSensor.class, DataType.TIME_SERIES, false, "Lufttemperatur erhitzt", "Air temperature heated");

	public static DataPointImpl humidityHeated 
		= new DataPointImpl("humHeated", HumiditySensor.class, DataType.TIME_SERIES, false, "Luftfeuchtigkeit erhitzt", "Air humidity heated");

	public static DataPointImpl dewPoint 
		= new DataPointImpl("dewPointTemp", TemperatureSensor.class, DataType.TIME_SERIES, false, "Taupunkttemperatur", "Dew point temperature");
	
}
