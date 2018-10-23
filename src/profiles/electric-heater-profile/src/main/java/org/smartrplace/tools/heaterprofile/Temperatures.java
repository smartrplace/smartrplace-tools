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
