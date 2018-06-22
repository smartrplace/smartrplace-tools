package org.smartrplace.tools.time.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;

/**
 * A {@link Timer} that is executed periodically according to a fixed {@link TemporalAmount}.
 * It can be used, for instance, to create a timer that runs 
 * <ul>
 *   <li>every day at 5pm, taking into account daylight savings, or
 *   <li>on the third day of each month, irrespectively of the number of days per month.
 * </ul>
 * Furthermore, the timer ensures that no offset aggregates over time due to the execution time of the timer listeners.
 * To final the appropriate start time for an aligned interval, use 
 * {@link TimeUtils#getNextAlignedIntervalStart(Instant, long, java.time.temporal.TemporalUnit, ZoneId)}
 * and related methods in {@link TimeUtils}.
 * 
 * <br>
 * 
 * Create an instance using the {@link #builder(ApplicationManager, TemporalAmount)} method. For instance:
 * <code>
 *    Instant start = TimeUtils.getNextAlignedIntervalStart(Instant.ofEpochMilli(System.currentTimeMillis()), 15, ChronoUnit.MINUTES);
 *    Timer timer = PeriodTimerSimple.builder(appMan, Duration.ofMinutes(15))
			.setStartTime(start)
			.build();
 * </code>
 * will execute every 15 minutes at the full hour, 15 past, 30 past and 45.
 * 
 * @author cnoelle
 */
public class PeriodTimerSimple extends DelegatingTimer implements PeriodTimer {
	
	private final ApplicationManager appMan;
	private final Callable<Void> resumeTask;
	final TemporalAmount period;
	final ZoneId zone;
	final Instant start;
	volatile Instant end; // may be null
	
	PeriodTimerSimple(
			final ApplicationManager appMan, 
			final TemporalAmount period,
			final Instant start,
			final Instant end,
			final ZoneId zone, 
			final Collection<TimerListener> listeners) {
		super(Objects.requireNonNull(appMan).createTimer(Long.MAX_VALUE));
		this.appMan = appMan;
		this.period = Objects.requireNonNull(period);
		this.zone = Objects.requireNonNull(zone);
		final long now = appMan.getFrameworkTime();
		this.start = start != null ? start : Instant.ofEpochMilli(now);
		this.end = end;
		if (end != null && end.compareTo(this.start) < 0) // should already be caught in builder
			throw new IllegalArgumentException("End time less than start time: " + this.start + " - " + end);
		Objects.requireNonNull(appMan);
		final PeriodTimerListener ownListener = new PeriodTimerListener(this);
		baseTimer.addListener(ownListener);
		if (listeners != null)
			listeners.forEach(l -> baseTimer.addListener(l));
		this.resumeTask = () -> {
			ownListener.timerElapsed(baseTimer);
			return (Void) null;
		};
		appMan.submitEvent(resumeTask);
	}
	
	public static PeriodTimerBuilder builder(ApplicationManager appMan, TemporalAmount period) {
		Objects.requireNonNull(appMan);
		Objects.requireNonNull(period);
		return new PeriodTimerBuilder(appMan, period);
	}

	@Override
	public TemporalAmount getPeriod() {
		return period;
	}
	
	/**
	 * Get the end time for the timer, after which it will be destroyed.
	 * May be null.
	 * @return
	 */
	@Override
	public Instant getEndTime() {
		return end;
	}
	
	/**
	 * Set the end time for the timer, after which it will be destroyed.
	 * @param end
	 */
	@Override
	public void setEndTime(Instant end) {
		this.end = end;
		if (end != null) {
			final long now = baseTimer.getExecutionTime();
			if (end.toEpochMilli() <= now) {
				destroy();
			} 
		}
	}

	@Override
	public List<TimerListener> getListeners() {
		return baseTimer.getListeners().stream()
				.filter(l -> !(l instanceof PeriodTimerListener))
				.collect(Collectors.toList());
	}
	
	@Override
	public void resume() {
		if (!baseTimer.isRunning())
			appMan.submitEvent(resumeTask);
	}

	private static class PeriodTimerListener implements TimerListener {
		
		private final PeriodTimerSimple master;
		private final boolean isDurationBased;
		Instant nextTargetExecution;
		
		PeriodTimerListener(PeriodTimerSimple master) {
			this.master = master;
			this.isDurationBased = master.period instanceof Duration;
			this.nextTargetExecution = master.start;
		}

		// calculate next execution time
		@Override
		public void timerElapsed(Timer timer) {
			timer.stop();
			final Instant now = Instant.ofEpochMilli(timer.getExecutionTime());
			Instant next = nextTargetExecution;
			if (isDurationBased) {
				while (next.compareTo(now) <= 0) {
					next = next.plus(master.period);
				}
			} else {
				final ZonedDateTime nowZdt = ZonedDateTime.ofInstant(now, master.zone);
				ZonedDateTime nextZdt = ZonedDateTime.ofInstant(next, master.zone);
				while (nextZdt.compareTo(nowZdt) <= 0) {
					nextZdt = nextZdt.plus(master.period);
				}
				next = nextZdt.toInstant();
			}
			final Instant end = master.end;
			if (end != null && next.compareTo(end) > 0) {
				timer.destroy();
				return;
			}
			// here, next is greater than now
			nextTargetExecution = next;
			final long diff = next.toEpochMilli() - now.toEpochMilli();
			timer.setTimingInterval(diff);
			timer.resume();
		}
		
	}
	
	public static class PeriodTimerBuilder {
		
		private final ApplicationManager appMan;
		private final TemporalAmount period;
		private List<TimerListener> listeners;
		private Instant start;
		private Instant end;
		private ZoneId zone = ZoneId.systemDefault();
		
		PeriodTimerBuilder(ApplicationManager appMan, TemporalAmount period) {
			this.appMan = appMan;
			this.period = period;
		}
		
		/**
		 * Create the timer.
		 * @return
		 */
		public PeriodTimerSimple build() {
			return new PeriodTimerSimple(appMan, period, start, end, zone, listeners);
		}
		
		/**
		 * Set a start instant, when the timer will be executed for the first time.
		 * If this is not set (null), the first execution will take place after the duration of 
		 * one period.
		 * @param start
		 * @return
		 * @throws IllegalArgumentException if {@link #setEndTime(Instant) end time} is set as well, and is less than the start time
		 */
		public PeriodTimerBuilder setStartTime(Instant start) {
			if (start != null && end != null && start.compareTo(end) > 0)
				throw new IllegalArgumentException("End time less than start time: " + start + " - " + end);
			this.start = start;
			return this;
		}
		
		/**
		 * Set an end time for the timer, until which it will be destroyed. If this is not set (null),
		 * the timer continues to run forever.
		 * @param end
		 * @return
		 * @throws IllegalArgumentException if {@link #setStartTime(Instant) start time} is set as well, and is greater than the end time
		 */
		public PeriodTimerBuilder setEndTime(Instant end) {
			if (start != null && end != null && start.compareTo(end) > 0)
				throw new IllegalArgumentException("End time less than start time: " + start + " - " + end);
			this.end = end;
			return this;
		}
		
		public PeriodTimerBuilder setTimeZone(ZoneId zone) {
			this.zone = Objects.requireNonNull(zone);
			return this;
		}
		
		public PeriodTimerBuilder addListener(TimerListener listener) {
			Objects.requireNonNull(listener);
			if (listeners == null)
				listeners = new ArrayList<>(4);
			listeners.add(listener);
			return this;
		}
		
	}
	
	
}
