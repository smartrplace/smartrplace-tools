package org.smartrplace.tools.time.utils.model;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;

/**
 * Configuration type for PeriodTimerPersistent.
 */
public interface TimerData extends Resource {

	TimeInterval period();
	TimeResource startTime();
	TimeResource endTime();
	StringResource timeZone();
	/**
	 * internal state variable, do not write
	 * @return
	 */
	TimeResource nextExecutionTime();
	
}
