package org.smartrplace.tools.profiles.prefs;

import java.util.Collection;
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
		return storeProfileConfiguration(template, id, resourceSettings, null, null);
	}
	
	Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings,
			State endState,
			OnOffSwitch onOffSwitch);
	
	Future<ProfileData> loadProfileConfiguration(
			ProfileTemplate template,
			String id);
	
	Future<Collection<String>> getProfileIds(String templateId);
	
}
