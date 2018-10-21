package org.smartrplace.tools.profiles.prefs;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.ogema.core.model.Resource;
import org.ogema.model.actors.OnOffSwitch;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;

public interface ProfilePreferences {
	
	/**
	 * @param template
	 * 		the profile template the configuration applies to
	 * @param id
	 * 		a unique (per template) id; if the id is already in use, the configuration
	 * 		is replaced
	 * @param resourceSettings
	 * 		the resource values
	 */
	default Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings) {
		return storeProfileConfiguration(template, id, resourceSettings, null, null, null);
	}
	
	/**
	 * 	
	 * @param template
	 * 		the profile template the configuration applies to
	 * @param id
	 * 		a unique (per template) id; if the id is already in use, the configuration
	 * 		is replaced
	 * @param resourceSettings
	 * 		the resource values
	 * @param durations
	 * 		must be either null or of the same size as {@link ProfileTemplate#states() template#states}, 
	 * 		with states in the same order
	 * @param endState
	 * 		may be null
	 * @param onOffSwitch
	 * 		may be null
	 * @return
	 * @throws IllegalArgumentException if durations is not null and its size does not match the size 
	 * 		of {@link ProfileTemplate#states() template#states}.
	 */
	Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings,
			List<StateDuration> durations,
			State endState,
			OnOffSwitch onOffSwitch);
	
	Future<ProfileData> loadProfileConfiguration(
			ProfileTemplate template,
			String id);
	
	Future<Collection<String>> getProfileIds(String templateId);
	
}
