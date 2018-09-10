package org.smartrplace.tools.exec;

import java.time.temporal.ChronoUnit;

// TODO capabilities and requirements
public class ExecutorConstants {

	/**
	 * Service property for {@link Runnable} services. 
	 * Default unit is milliseconds, unless otherwise specified via 
	 * {@link #TASK_PROPERTIES_UNIT}.
	 */
	public static final String TASK_PERIOD = "org.smartrplace.tools.housekeeping.Period";

	/**
	 * Service property for {@link Runnable} services.
	 * Default unit is milliseconds, unless otherwise specified via 
	 * {@link #TASK_PROPERTIES_UNIT}.
	 */
	public static final String TASK_DELAY = "org.smartrplace.tools.housekeeping.Delay";
	
	/**
	 * Time unit for {@link #TASK_PERIOD} and {@link #TASK_DELAY} properties. If not specified,
	 * milliseconds are assumed. Allowed values are the names of the enum constants in {@link ChronoUnit},
	 * such as "MILLIS", "SECONDS", "MINUTES", "HOURS", "DAYS", "WEEKS", "MONTHS" (case-insensitive). If
	 * the value is invalid, the task will be ignored.<br>
	 * Note that date-based units will be treated as if they had a fixed duration, e.g. one month will always be treated
	 * as having 30 days, etc.
	 */
	public static final String TASK_PROPERTIES_TIME_UNIT = "org.smartrplace.tools.housekeeping.Unit";
	
	public static final String HOUSEKEEPING_EXEC_PID = "org.smartrplace.tools.exec.Housekeeping";	
	
}