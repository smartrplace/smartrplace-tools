package org.smartrplace.tools.profiles.prefs.model;

import java.time.temporal.ChronoUnit;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Configuration;

public interface StateDuration extends Configuration {

	StringResource stateId();
	IntegerResource duration();
	/**
	 * Must be the name of a {@link ChronoUnit} enum.
	 * @return
	 */
	StringResource timeUnit();
	
}
