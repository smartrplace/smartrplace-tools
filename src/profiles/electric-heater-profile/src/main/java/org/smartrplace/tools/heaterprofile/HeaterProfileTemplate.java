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
		final ReadOnlyTimeSeries tempOutside = (ReadOnlyTimeSeries) contextData.get(StandardDataPoints.outsideTemperature(false));
		final ReadOnlyTimeSeries humidityOutside = (ReadOnlyTimeSeries) contextData.get(StandardDataPoints.outsideHumidity(false));
		final MultiTimeSeriesIterator it = MultiTimeSeriesIteratorBuilder.newBuilder(Arrays.asList(
					tempInside.iterator(),
					humInside.iterator(),
					tempOutside.iterator(),
					humidityOutside.iterator()
				))
				.setGlobalInterpolationMode(InterpolationMode.LINEAR)
				.build();
		final FloatTimeSeries result = new FloatTreeTimeSeries();
		while (it.hasNext()) {
			final SampledValueDataPoint point = it.next();
			final float tIn = point.getElement(0).getValue().getFloatValue();
			final float hIn = point.getElement(1).getValue().getFloatValue();
			final float tOut = point.getElement(2).getValue().getFloatValue();
			final float hOut = point.getElement(3).getValue().getFloatValue();
			// TODO calculate dew point temperature
			final float dewPoint = (tIn + tOut) / 2 * hIn / hOut; // nonsense formula
			result.addValue(point.getTimestamp(), new FloatValue(dewPoint));
		}
		return Collections.singletonMap(Temperatures.dewPoint, result);
	}
	
}
