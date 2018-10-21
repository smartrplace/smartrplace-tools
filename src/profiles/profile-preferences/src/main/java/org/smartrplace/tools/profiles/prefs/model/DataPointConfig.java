package org.smartrplace.tools.profiles.prefs.model;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Configuration;

public interface DataPointConfig extends Configuration {

	StringResource dataPointId();
	/**
	 * A reference to the target resource
	 * @return
	 */
	Resource target();
	
}
