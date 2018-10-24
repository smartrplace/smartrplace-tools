package org.smartrplace.tools.heaterprofile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.timeseries.api.FloatTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;
import org.ogema.tools.timeseries.iterator.api.MultiTimeSeriesIterator;
import org.ogema.tools.timeseries.iterator.api.MultiTimeSeriesIteratorBuilder;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;
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
	
	private static final float b = 18.678F;
	private static final float c = 257.14F;
	
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
		return Arrays.asList(StateImpl.OFF, StateImpl.ON, StateImpl.OFF);
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
				StandardDataPoints.roomTemperature(false), 
				StandardDataPoints.roomHumidity(false),
				StandardDataPoints.powerConsumption(false),
				StandardDataPoints.roomInfo(true)
		);
	}
	
	@Override
	public List<DataPoint> derivedData() {
		return Arrays.asList(Temperatures.dewPoint);
	}
	
	@Override
	public Map<DataPoint, Object> derivedData(Map<DataPoint, Object> primaryData, Map<DataPoint, Object> contextData) {
		final ReadOnlyTimeSeries tempInside = (ReadOnlyTimeSeries) primaryData.get(Temperatures.temperatureHeated);
		final ReadOnlyTimeSeries humInside = (ReadOnlyTimeSeries) primaryData.get(Temperatures.humidityHeated);
		final MultiTimeSeriesIterator it = MultiTimeSeriesIteratorBuilder.newBuilder(Arrays.asList(
					tempInside.iterator(),
					humInside.iterator()
				))
				.setGlobalInterpolationMode(InterpolationMode.LINEAR)
				.build();
		final FloatTimeSeries result = new FloatTreeTimeSeries();
		while (it.hasNext()) {
			final SampledValueDataPoint point = it.next();
			final float tIn = point.getElement(0).getValue().getFloatValue() - 273.15F;
			final float hIn = point.getElement(1).getValue().getFloatValue();
			// TODO calculate dew point temperature
			// https://en.wikipedia.org/wiki/Dew_point
			final float gamma = (float) (Math.log(hIn) + b * tIn / (c + tIn));
			final float dewPoint = c * gamma / (b - gamma); // in °C
			result.addValue(point.getTimestamp(), new FloatValue(dewPoint + 273.15F));
		}
		return Collections.singletonMap(Temperatures.dewPoint, result);
	}
	
}
