package org.smartrplace.tools.profiles.utils;

import org.ogema.model.locations.Room;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.tools.profiles.DataPoint.DataType;

public class StandardDataPoints {

	private StandardDataPoints() {}
	
	public static DataPointImpl outsideTemperature(boolean optional) {
		return new DataPointImpl("outsideTemperature", TemperatureSensor.class, DataType.TIME_SERIES, 
				optional, "Außentemperatur", "Outside temperature");
	}
	
	public static DataPointImpl outsideHumidity(boolean optional) {
		return new DataPointImpl("outsideHumidity", HumiditySensor.class, DataType.TIME_SERIES, 
				optional, "Luftfeuchtigkeit außen", "Outside humidity");
	}
	
	public static DataPointImpl roomInfo(boolean optional) {
		return new DataPointImpl("roomInfo", Room.class, DataType.STRING, optional, "Raum", "Room");		
	}
	
	public static DataPointImpl powerConsumption(boolean optional) {
		return new DataPointImpl("power", PowerSensor.class, DataType.TIME_SERIES, optional, "Leistung / W", "Power / W");		
	}
}
