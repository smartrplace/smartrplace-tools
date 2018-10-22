package org.smartrplace.tools.profiles.utils;

import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.DataPoint.DataType;

public class StandardDataPoints {

	private static final String START_TIME_ID = "startTime";
	
	private StandardDataPoints() {}
	
	public static boolean isStartTime(final DataPoint dp) {
		return dp != null && START_TIME_ID.equals(dp.id());
	}
	
	public static DataPointImpl profileStartTime(boolean optional) {
		return new DataPointImpl(
			START_TIME_ID, TimeResource.class, DataType.SINGLE_VALUE, optional, "Startzeit", "start time");
	}
	
	public static DataPointImpl outsideTemperature(boolean optional) {
		return new DataPointImpl("outsideTemperature", TemperatureSensor.class, DataType.TIME_SERIES, 
				optional, "Außentemperatur", "Outside temperature");
	}
	
	public static DataPointImpl outsideHumidity(boolean optional) {
		return new DataPointImpl("outsideHumidity", HumiditySensor.class, DataType.TIME_SERIES, 
				optional, "Luftfeuchtigkeit außen", "Outside humidity");
	}
	
	public static DataPointImpl roomTemperature(boolean optional) {
		return new DataPointImpl("roomTemperature", TemperatureSensor.class, DataType.TIME_SERIES, 
				optional, "Raumtemperatur", "Room temperature");
	}
	
	public static DataPointImpl roomHumidity(boolean optional) {
		return new DataPointImpl("roomHumidity", HumiditySensor.class, DataType.TIME_SERIES, 
				optional, "Raum-Luftfeuchtigkeit", "Room air humidity");
	}
	
	public static DataPointImpl roomInfo(boolean optional) {
		return new DataPointImpl("roomInfo", Room.class, DataType.STRING, optional, "Raum", "Room");		
	}
	
	public static DataPointImpl powerConsumption(boolean optional) {
		return new DataPointImpl("power", PowerSensor.class, DataType.TIME_SERIES, optional, "Leistung / W", "Power / W");		
	}
}
