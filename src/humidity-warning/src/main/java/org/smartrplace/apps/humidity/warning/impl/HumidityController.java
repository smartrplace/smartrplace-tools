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
package org.smartrplace.apps.humidity.warning.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.DoubleStream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceStructureEvent;
import org.ogema.core.resourcemanager.ResourceStructureListener;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.apps.humidity.warning.impl.pattern.HumidityPattern;
import org.smartrplace.apps.humidity.warning.impl.pattern.WarningConfigurationPattern;

import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.services.MessagingService;
import de.iwes.widgets.api.services.NameService;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

class HumidityController implements ResourceValueListener<FloatResource>, TimerListener {

	private static final long DEFAULT_MIN_WARN_ITV = 3600000; // 1h
	private final static DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final ApplicationManager appMan;
	private final MessagingService messagingService;
	private final NameService nameService;
	private final Map<String, HumidityPattern> sensors = new HashMap<>(4);
//	private final Map<String, TemperaturePattern> tempSensors = new HashMap<>(4);
	private final WarningConfigurationPattern config;
	private final Thresholdlistener thresholdListener = new Thresholdlistener(this);
	
	// state
	// may be Float.NaN, e.g. if no sensors are available for this room
	private float lastHumidityMax;
	private float lastHumidityMin;
	private float lastDewPointCelsius;
	
	private final AtomicBoolean closed = new AtomicBoolean(false);
//	private long lastUpdateTime = Long.MAX_VALUE;
	private long upperViolatingSince = Long.MAX_VALUE;
	private long lowerViolatingSince = Long.MAX_VALUE;
	private Timer timer; // created on demand

	HumidityController(
			WarningConfigurationPattern config, 
			ApplicationManager appMan, 
			MessagingService messaging,
			NameService nameService) {
		this.config = config;
		this.appMan = appMan;
		this.messagingService = messaging;
		this.nameService = nameService;
		config.lowerThresholdHumidity.addStructureListener(thresholdListener);
		config.lowerThresholdHumidity.addValueListener(thresholdListener);
		config.upperThresholdHumidity.addStructureListener(thresholdListener);
		config.upperThresholdHumidity.addValueListener(thresholdListener);
	}

	WarningConfigurationPattern getConfig() {
		return config;
	}
	
	void valueChanged() {
		if (closed.get() || sensors.isEmpty())
			return;
		final float maxHumidity = getMaxHumidity(this, true);
		final float minHumidity = getMaxHumidity(this, false);
		if (Float.isFinite(maxHumidity) && Float.isFinite(minHumidity))
			checkWarning(maxHumidity, minHumidity);
		this.lastHumidityMax = maxHumidity;
		this.lastHumidityMin = minHumidity;
	}
	
	private void checkWarning(final float maxHumidity, final float minHumidity) {
		final float upperThreshold = config.upperThresholdHumidity.isActive() ? config.upperThresholdHumidity.getValue() : Float.NaN;
		final float lowerThreshold = config.lowerThresholdHumidity.isActive() ? config.lowerThresholdHumidity.getValue() : Float.NaN;
		final boolean isHighHumidity = Float.isFinite(upperThreshold) && maxHumidity > upperThreshold;
		final boolean isLowHumidity = Float.isFinite(lowerThreshold) && maxHumidity < lowerThreshold;
		final long time = appMan.getFrameworkTime();
		final long minItv = getMinInterval();
		boolean highWarning = isHighHumidity && !config.model.isHigh()
				&& (!config.lastWarningHighHumidity.isActive() || time - config.lastWarningHighHumidity.getValue() > minItv); // send at most once per hour
		boolean lowWarning = isLowHumidity && !config.model.isLow()
				&& (!config.lastWarningLowHumidity.isActive() || time - config.lastWarningLowHumidity.getValue() > minItv);
		if (!isHighHumidity)
			config.model.setHigh(false);
		if (!isLowHumidity)
			config.model.setLow(false);
		long nextExecHigh = Long.MAX_VALUE;
		if (highWarning) {
			final long timeout = config.upperTimeout.isActive() ? config.upperTimeout.getValue() : 0;
			if (upperViolatingSince == Long.MAX_VALUE) {
				upperViolatingSince = time;
				if (timeout > 0) {
					highWarning = false;
					nextExecHigh = timeout + 1000;
				}
			} else if (time < upperViolatingSince + timeout) {
				highWarning = false; // wait to confirm the violation
				nextExecHigh = upperViolatingSince + timeout - time + 1000; 
			} else {
				upperViolatingSince = Long.MAX_VALUE;
			}
		} else {
			upperViolatingSince = Long.MAX_VALUE;
		}
		long nextExecLow = Long.MAX_VALUE;
		if (lowWarning) {
			final long timeout = config.lowerTimeout.isActive() ? config.lowerTimeout.getValue() : 0;
			if (lowerViolatingSince == Long.MAX_VALUE) {
				lowerViolatingSince = time;
				if (timeout > 0) {
					nextExecLow = timeout + 1000;
					lowWarning = false;
				}
			} else if (time < lowerViolatingSince + timeout) {
				lowWarning = false; // wait to confirm the violation
				nextExecLow = lowerViolatingSince + timeout - time + 1000;
			} else {
				lowerViolatingSince = Long.MAX_VALUE;
			}
		} else
			lowerViolatingSince = Long.MAX_VALUE;
		if (lowWarning || highWarning) {
			final StringBuilder messageBuilder = new StringBuilder();
			if (highWarning) {
				appendHumidityLevelWarning((int) (maxHumidity*100), (int) (upperThreshold*100),  true, time, messageBuilder);
				config.model.setHigh(true);
			}
			if (lowWarning) {
				if (highWarning)
					messageBuilder.append("\r\n"); // XXX?
				appendHumidityLevelWarning((int) (minHumidity*100), (int) (lowerThreshold*100), false, time, messageBuilder);
				config.model.setLow(true);
			}
			sendWarning(messageBuilder, MessagePriority.MEDIUM);
			if (lowWarning) {
				config.lastWarningLowHumidity.<TimeResource> create().setValue(time);
				config.lastWarningLowHumidity.activate(false);
			}
			if (highWarning) {
				config.lastWarningHighHumidity.<TimeResource> create().setValue(time);
				config.lastWarningHighHumidity.activate(false);
			}
		}
		final long next = Math.min(nextExecHigh, nextExecLow);
		if (next != Long.MAX_VALUE) {
			if (timer == null) {
				timer = appMan.createTimer(next, this);
			} else {
				timer.setTimingInterval(next);
				timer.resume();
			}
		} else if (timer != null) {
			timer.destroy();
			timer = null;
		}
	}
	
	private long getMinInterval() {
		if (config.model.minWarningInterval().isActive())
			return config.model.minWarningInterval().getValue();
		return DEFAULT_MIN_WARN_ITV;
	}
	
	private void appendHumidityLevelWarning(final int value, final int threshold, final boolean highOrLow, final long time, final StringBuilder sb) {
		sb.append("Humidity in room ")
			.append(Utils.getName(config.room.getLocationResource(), nameService, OgemaLocale.ENGLISH))
			.append(" has ");
		if (highOrLow)
			sb.append("exceeded");
		else
			sb.append("fallen below");
		sb.append(" the threshold value ").append(threshold).append("%. New value: ").append(value).append('%')
			.append(", time: ");
		final ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
		sb.append(formatter.format(zdt)).append('.');
	}
	
	private void sendWarning(StringBuilder msg, final MessagePriority prio) {
		final MessageImpl message = new MessageImpl("Humidity warning", msg.toString(), prio);
		messagingService.sendMessage(appMan, message);
	}
	
	void addSensor(final HumidityPattern sensor, final boolean triggerCheck) {
		if (closed.get())
			return;
		sensors.put(sensor.model.getLocation(), sensor);
		sensor.reading.addValueListener(this);
		if (triggerCheck)
			valueChanged();
	}
	
	void removeSensor(final HumidityPattern sensor) {
		if (closed.get())
			return;
		if (sensors.remove(sensor.model.getLocation()) != null) {
			sensor.reading.removeValueListener(this);
			valueChanged();
		}
	}
	
	void close() {
		if (closed.getAndSet(true))
			return;
		sensors.values().forEach(sensor -> sensor.reading.removeValueListener(this));
		sensors.clear();
		config.lowerThresholdHumidity.removeStructureListener(thresholdListener);
		config.lowerThresholdHumidity.removeValueListener(thresholdListener);
		config.upperThresholdHumidity.removeStructureListener(thresholdListener);
		config.upperThresholdHumidity.removeValueListener(thresholdListener);
		if (timer != null) {
			timer.destroy();
			timer = null;
		}
	}
	
	@Override
	public void resourceChanged(FloatResource resource) {
		valueChanged();
	}
	
	@Override
	public void timerElapsed(Timer timer) {
		timer.stop();
		valueChanged();
	}
	
	private final static float getMaxHumidity(final HumidityController controller, final boolean maxOrMin) {
		final BooleanResource avgres = controller.config.model.useRoomAverage();
		final boolean avg = avgres.isActive() ? avgres.getValue() : false;
		final DoubleStream ds =  controller.sensors.values().stream()
				.mapToDouble(sensor -> sensor.reading.getValue())
				.filter(value -> value >= 0 && value <= 1);
		final OptionalDouble opt = avg ? ds.average() : maxOrMin ? ds.max() : ds.min();
		return (float) (opt.isPresent() ? opt.getAsDouble() : Float.NaN);
	}
	
	private static class Thresholdlistener implements ResourceStructureListener, ResourceValueListener<FloatResource> {
		
		private final HumidityController controller;
		
		public Thresholdlistener(HumidityController controller) {
			this.controller = controller;
		}

		@Override
		public void resourceChanged(FloatResource resource) {
			controller.valueChanged();
		}

		@Override
		public void resourceStructureChanged(ResourceStructureEvent event) {
			switch (event.getType()) {
			case RESOURCE_ACTIVATED:
			case RESOURCE_CREATED:
				controller.valueChanged();
			default:
			}
		}
		
	}
	
}
