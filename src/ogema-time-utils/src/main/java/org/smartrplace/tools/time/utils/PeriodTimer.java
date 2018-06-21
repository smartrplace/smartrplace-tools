package org.smartrplace.tools.time.utils;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

import org.ogema.core.application.Timer;

/**
 * A {@link Timer} that is executed periodically according to a fixed {@link TemporalAmount}.
 * 
 * @author cnoelle
 */
public interface PeriodTimer extends Timer {

	/**
	 * May return null, if the period is not set. In this case the timer should be stopped, too.
	 * @return
	 */
	TemporalAmount getPeriod();
	
	/**
	 * Null if end time is not set, or the last execution time before this timer will be destroyed.
	 * @return
	 */
	Instant getEndTime();
	
	/**
	 * Set the last execution time, after which the timer will be destroyed.
	 * @param endTime
	 */
	void setEndTime(Instant endTime);
	
	
}
