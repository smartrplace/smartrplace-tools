package org.smartrplace.tools.profiles.prefs.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.prototypes.Configuration;
import org.smartrplace.tools.profiles.ProfileTemplate;

public interface ProfileConfiguration extends Configuration {

	/**
	 * The {@link ProfileTemplate#id() id} of a {@link ProfileTemplate}. 
	 * @return
	 */
	StringResource profileTemplateId();
	
	/**
	 * Must be unique per {@link #profileTemplateId()}
	 * @return
	 */
	StringResource id();
	
	ResourceList<DataPointConfig> dataPoints();
	
	OnOffSwitch onOffSwitch();
	
	StringResource endState();
	
	/**
	 * Order of durations must correspond to the order of states in
	 * {@link ProfileTemplate#states()}.
	 * @return
	 */
	ResourceList<StateDuration> durations();
	
}