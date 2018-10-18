package org.smartrplace.tools.heaterprofile;

import java.util.Arrays;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.utils.StandardDataPoints;
import org.smartrplace.tools.profiles.utils.StateImpl;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

@Component(
		service = ProfileTemplate.class,
		property = {
				ProfileTemplate.ID_PROPERTY + "=electricHeating",
				ProfileTemplate.LABEL_PROPERTY + "=Electric heating"
		}
)
public class HeaterProfileTemplate implements ProfileTemplate {
	
	@Override
	public String id() {
		return "electricHeating";
	}
	
	@Override
	public String label(OgemaLocale locale) {
		if (locale == OgemaLocale.GERMAN)
			return "Elektrische Heizung";
		return "Electric heater profile";
	}
	
	@Override
	public List<State> states() {
		return Arrays.asList(StateImpl.OFF, StateImpl.ON);
	}

	@Override
	public List<DataPoint> primaryData() {
		return Arrays.asList(Temperatures.temperatureHeated, Temperatures.humidityHeated);
	}

	@Override
	public List<DataPoint> contextData() {
		return Arrays.asList(
				StandardDataPoints.profileStartTime(false),
				StandardDataPoints.outsideTemperature(false), 
				StandardDataPoints.outsideHumidity(false),
				StandardDataPoints.powerConsumption(false),
				StandardDataPoints.roomInfo(true)
		);
	}
	
}
