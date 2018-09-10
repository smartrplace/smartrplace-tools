package org.smartrplace.tools.exec.impl;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
		description="Configuration for the housekeeping executor service. This service executes short-lived and seldom-running "
		 	+ "clean-up tasks. The configuration in particular defines a minimum period for submitted tasks; tasks with a smaller "
			+ "period are rejeceted."
)
public @interface HousekeepingServiceConfig {
	
	/**
	 * The housekeeping executor will not accept tasks with a period
	 * below this value (in ms). Default: 1, i.e. no restriction.
	 * @return
	 */
	long minPeriodMs() default 1;
	
	/**
	 * When the service is deactivated it will wait for tasks already running for 
	 * this long. Set to 0 or a negative value to disable waiting completely. 
	 * Default 2000, i.e. 2s.
	 * @return
	 */
	long waitTimeOnShutdownMs() default 2000;
	
}
