package org.smartrplace.tools.time.utils.model;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;

/**
 * Configuration type for PeriodTimerPersistent.
 */
public interface TimerData extends Resource {

	TemporalAmountResource period();
	TimeResource startTime();
	TimeResource endTime();
	StringResource timeZone();
	TimeResource nextExecutionTime();
	
}
