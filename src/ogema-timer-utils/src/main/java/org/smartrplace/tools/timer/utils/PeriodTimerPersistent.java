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
package org.smartrplace.tools.timer.utils;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.AccessMode;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.ResourceOperationException;
import org.ogema.core.resourcemanager.ResourceStructureEvent;
import org.ogema.core.resourcemanager.ResourceStructureListener;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.transaction.ReadConfiguration;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.core.resourcemanager.transaction.TransactionFuture;
import org.slf4j.Logger;
import org.smartrplace.tools.timer.utils.model.TimeInterval;
import org.smartrplace.tools.timer.utils.model.TimerData;

/**
 * A {@link PeriodTimer} whose configuration data is stored persistently in OGEMA resources
 * (see the resource type {@link TimerData}). 
 * Note that the timer itself is not persisted, i.e. after a restart of the app the
 * timer must be created anew.
 */
public class PeriodTimerPersistent extends DelegatingTimer implements PeriodTimer {

	final ApplicationManager appMan;
	final Callable<Void> resumeTask;
	private final ChangeListener changeListener;
	final TimerData config;
	final Logger logger;
	volatile boolean active = false;
	
	/**
	 * Create a persistent timer. This constructor should be used with ready configured resources,
	 * in particular the following values should be set prior to creating the timer:
	 * <ul>
	 *   <li>{@link TimerData#startTime()} (if required at all)
	 *   <li>{@link TimerData#period()} and its subresources.
	 * </ul>
	 * @param appMan
	 * @param config
	 * @throws NullPointerException if appMan or config are null
	 */
	public PeriodTimerPersistent(final ApplicationManager appMan, final TimerData config) {
		this(appMan, config, null);
	}
	
	/**
	 * Create a persistent timer. This constructor should be used with ready configured resources,
	 * in particular the following values should be set prior to creating the timer:
	 * <ul>
	 *   <li>{@link TimerData#startTime()} (if required at all)
	 *   <li>{@link TimerData#period()} and its subresources.
	 * </ul>
	 * @param appMan
	 * @param config
	 * @param listener
	 * @throws NullPointerException if appMan or config are null
	 */
	public PeriodTimerPersistent(
			final ApplicationManager appMan, 
			final TimerData config,
			final TimerListener listener) {
		this(appMan, config, null, 0, null, listener);
	}
	
	/**
	 * Create a persistent timer. This constructor should be used with newly created configuration resources
	 * whose {@link TimerData#period()} and {@link TimerData#startTime()} resources have not been initialized yet.<br>
	 * Note that the config resource will be activated recursively by calling this constructor.
	 * @param appMan
	 * @param config
	 * @param timeUnit
	 * @param timeFactor
	 * @param start
	 * 		may be null; first execution time of the timer
	 * @param listener
	 *  	may be null, in which case no listener is added to the timer.
	 * @throws NullPointerException if appMan, config or timeUnit are null
	 * @throws IllegalArgumentException if timeFactor is non-positive
	 */
	public PeriodTimerPersistent(
			final ApplicationManager appMan, 
			final TimerData config,
			final ChronoUnit timeUnit,
			final int timeFactor,
			final Instant start, 
			final TimerListener listener) {
		super(Objects.requireNonNull(appMan).createTimer(Long.MAX_VALUE));
		this.appMan = appMan;
		this.config = Objects.requireNonNull(config);
		config.nextExecutionTime().requestAccessMode(AccessMode.EXCLUSIVE, AccessPriority.PRIO_HIGHEST);
		this.logger = appMan.getLogger();
		if (timeUnit != null && timeFactor <= 0) {
			baseTimer.destroy();
			throw new IllegalArgumentException("Time factor must be positive, got " + timeFactor);
		}
		if (timeUnit != null) {
			config.period().chronoUnit().<StringResource> create().setValue(timeUnit.name());
			config.period().factor().<IntegerResource> create().setValue(timeFactor);
		}
		if (start != null && !config.startTime().isActive()) {
			config.startTime().<TimeResource> create().setValue(start.toEpochMilli());
		}
		if (timeUnit != null)
			config.activate(true);
		final PeriodTimerListener ownListener = new PeriodTimerListener(this);
		baseTimer.addListener(ownListener);
		if (listener != null)
			baseTimer.addListener(listener);
		this.resumeTask = () -> {
			ownListener.timerElapsed(baseTimer);
			return (Void) null;
		};
		this.changeListener = new ChangeListener(this);
		config.addStructureListener(changeListener);
		if (config.isActive())
			resume();
	}
	
	
	
	private final void addChangeListener() {
		final TimeInterval period =config.period();
		period.chronoUnit().addStructureListener(changeListener);
		period.chronoUnit().addValueListener(changeListener);
		period.factor().addStructureListener(changeListener);
		period.factor().addValueListener(changeListener);
		config.endTime().addStructureListener(changeListener);
		config.endTime().addValueListener(changeListener);
	}
	
	private final void removeChangeListener() {
		final TimeInterval period =config.period();
		period.chronoUnit().removeStructureListener(changeListener);
		period.chronoUnit().removeValueListener(changeListener);
		period.factor().removeStructureListener(changeListener);
		period.factor().removeValueListener(changeListener);
		config.endTime().removeStructureListener(changeListener);
		config.endTime().removeValueListener(changeListener);
	}

	@Override
	public TemporalAmount getPeriod() {
		final ResourceTransaction trans = appMan.getResourceAccess().createResourceTransaction();
		final TimeInterval period = config.period();
		final TransactionFuture<Boolean> active0 = trans.isActive(period);
		final TransactionFuture<String> chrono = trans.getString(period.chronoUnit(), ReadConfiguration.FAIL);
		final TransactionFuture<Integer> fact = trans.getInteger(period.factor(), ReadConfiguration.FAIL);
		try {
			trans.commit();
		} catch (IllegalStateException e) {
			return null;
		} catch (ResourceOperationException e) {
			logger.warn("Failed to read timer period.", e.getCause());
			return null;
		}
		if (!active0.getValue())
			return null;
		try {
			final ChronoUnit unit = ChronoUnit.valueOf(chrono.getValue());
			final int factor = fact.getValue();
			if (!unit.isDateBased())
				return Duration.of(factor, unit);
			else { // XXX no standard method for this?
				switch (unit) {
				case DAYS:
					return Period.ofDays(factor);
				case WEEKS:
					return Period.ofWeeks(factor);
				case MONTHS:
					return Period.ofMonths(factor);
				case YEARS:
					return Period.ofYears(factor);
				case DECADES:
					return Period.ofYears(10 * factor);
				case CENTURIES:
					return Period.ofYears(100 * factor);
				default:
					throw new IllegalArgumentException("Unit " + unit + " not supported");
				}
			}
		} catch (IllegalArgumentException e) {
			logger.warn("Failed to read timer period",e);
			return null;
		}
		
	}

	@Override
	public Instant getEndTime() {
		final ResourceTransaction trans = appMan.getResourceAccess().createResourceTransaction();
		final TransactionFuture<Boolean> active = trans.isActive(config.endTime());
		final TransactionFuture<Long> endTime = trans.getTime(config.endTime());
		try {
			trans.commit();
		} catch (ResourceOperationException e) {
			logger.warn("Failed to read timer end time.", e.getCause());
			return null;
		}
		if (!active.getValue())
			return null;
		return Instant.ofEpochMilli(endTime.getValue());
	}

	@Override
	public void setEndTime(Instant endTime) {
		if (endTime == null) {
			config.endTime().deactivate(false);
			return;
		}
		config.endTime().<TimeResource> create().setValue(endTime.toEpochMilli());
		config.endTime().activate(false);
	}

	@Override
	public void resume() {
		if (!baseTimer.isRunning()) {
			addChangeListener();
			active = true;
			appMan.submitEvent(resumeTask);
		}
	}

	@Override
	public List<TimerListener> getListeners() {
		return baseTimer.getListeners().stream()
				.filter(l -> !(l instanceof PeriodTimerListener))
				.collect(Collectors.toList());
	}
	
	@Override
	public void stop() {
		removeChangeListener();
		active = false;
		super.stop();
	}
	
	@Override
	public void destroy() {
		try {
			config.removeStructureListener(changeListener);
			removeChangeListener();
			config.nextExecutionTime().requestAccessMode(AccessMode.SHARED, AccessPriority.PRIO_LOWEST);
		} catch (Exception e) {}
		super.destroy();
	}
	
	private static class ChangeListener implements ResourceValueListener<SingleValueResource>, ResourceStructureListener {
		
		private final PeriodTimerPersistent master;
		
		ChangeListener(PeriodTimerPersistent master) {
			this.master = master;
		}

		@Override
		public void resourceStructureChanged(ResourceStructureEvent event) {
			switch (event.getType()) {
			case RESOURCE_DELETED:
				boolean configExists = false;
				try {
					configExists = master.config.exists();
				} catch (NullPointerException e) {}
				if (!configExists)
					master.destroy();
				return;
				 // TODO write a test for activation and deactivation of the config resource
			case RESOURCE_ACTIVATED:
				if (master.config.equals(event.getChangedResource())) {
					master.resume();
					return;
				}
				break;
			case RESOURCE_DEACTIVATED:
				if (master.config.equals(event.getChangedResource())) {
					master.stop();
					return;
				}
				break;
			default:
				break;
			}
			master.appMan.submitEvent(master.resumeTask);
		}

		@Override
		public void resourceChanged(SingleValueResource resource) {
			master.appMan.submitEvent(master.resumeTask);
		}
		
	}
	
	private static class PeriodTimerListener implements TimerListener {
		
		private final TimerData config;
		private final PeriodTimerPersistent master;
		
		PeriodTimerListener(PeriodTimerPersistent master) {
			this.master = master;
			this.config = master.config;
		}
		
		private static TemporalAmount getTempAmount(final ChronoUnit unit, final int factor) {
			if (!unit.isDateBased())
				return Duration.of(factor, unit);
			else { // XXX no standard method for this?
				switch (unit) {
				case DAYS:
					return Period.ofDays(factor);
				case WEEKS:
					return Period.ofWeeks(factor);
				case MONTHS:
					return Period.ofMonths(factor);
				case YEARS:
					return Period.ofYears(factor);
				case DECADES:
					return Period.ofYears(10 * factor);
				case CENTURIES:
					return Period.ofYears(100 * factor);
				default:
					throw new IllegalArgumentException("Unit " + unit + " not supported");
				}
			}
		}

		// calculate next execution time
		@Override
		public void timerElapsed(Timer timer) {
			if (!master.active) {
				timer.stop();
				return;
			}
			timer.stop();
			final Instant now = Instant.ofEpochMilli(timer.getExecutionTime());
			final ResourceTransaction trans = master.appMan.getResourceAccess().createResourceTransaction();
			
			final TransactionFuture<Boolean> configActive = trans.isActive(config);
			final TimeInterval period = config.period();
			final TransactionFuture<String> unit = trans.getString(period.chronoUnit(), ReadConfiguration.FAIL);
			final TransactionFuture<Integer> fact = trans.getInteger(period.factor(), ReadConfiguration.FAIL);
			final TransactionFuture<Long> start = trans.getTime(config.startTime(), ReadConfiguration.RETURN_NULL);
			final TransactionFuture<Long> end = trans.getTime(config.endTime(), ReadConfiguration.RETURN_NULL);
			final TransactionFuture<Long> nextExec = trans.getTime(config.nextExecutionTime(), ReadConfiguration.RETURN_NULL);
			final TransactionFuture<String> timeZone = trans.getString(config.timeZone(), ReadConfiguration.RETURN_NULL);
			try {
				trans.commit();
			} catch (IllegalStateException e) { // inactive resources...
				return;
			} catch (ResourceOperationException e) {
				master.logger.warn("Failed to read timer config {}", config, e);
				return;
			}
			if (!configActive.getValue())
				return;
			final Long end0 = end.getValue();
			if (end0 != null && end0 <= now.toEpochMilli()) {
				master.destroy();
				return;
			}
			final ChronoUnit chrono;
			final TemporalAmount duration;
			try {
				chrono = ChronoUnit.valueOf(unit.getValue());
				final int factor = fact.getValue();
				if (factor <= 0) {
					master.logger.warn("Temporal amount factor is negative ({}) in config {}",factor, config);
					return;
				}
				duration = getTempAmount(chrono, factor);
			} catch (IllegalArgumentException e) {
				master.logger.warn("Invalid time unit {} in config {}", unit.getValue(), config);
				return;
			}
			Long baseTime = nextExec.getValue();
			if (baseTime == null)
				baseTime = start.getValue();
			if (baseTime == null) {
				baseTime = now.toEpochMilli();
				config.startTime().<TimeResource> create().setValue(baseTime);
				config.startTime().activate(false);
			}
			Instant next = Instant.ofEpochMilli(baseTime);
			if (!chrono.isDateBased()) {
				while (next.compareTo(now) <= 0) {
					next = next.plus(duration);
				}
			} else {
				ZoneId zone = null;
				try {
					zone = ZoneId.of(timeZone.getValue());
				} catch (DateTimeException | NullPointerException e) {
					zone = ZoneId.systemDefault();
				}
				final ZonedDateTime nowZdt = ZonedDateTime.ofInstant(now, zone);
				ZonedDateTime nextZdt = ZonedDateTime.ofInstant(next, zone);
				while (nextZdt.compareTo(nowZdt) <= 0) {
					nextZdt = nextZdt.plus(duration);
				}
				next = nextZdt.toInstant();
			}
			config.nextExecutionTime().<TimeResource> create().setValue(next.toEpochMilli());
			config.nextExecutionTime().activate(false);
			// here, next is greater than now
			final long diff = next.toEpochMilli() - now.toEpochMilli();
			timer.setTimingInterval(diff);
			timer.resume();
		}
		
	}
	
}
