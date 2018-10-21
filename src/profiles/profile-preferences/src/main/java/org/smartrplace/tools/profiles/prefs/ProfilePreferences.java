package org.smartrplace.tools.profiles.prefs;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import org.ogema.core.model.Resource;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;

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
	Future<?> storeProfileConfiguration(
			ProfileTemplate template,
			String id,
			Map<DataPoint, Resource> resourceSettings);
	
	Future<Map<DataPoint, Resource>> loadProfileConfiguration(
			ProfileTemplate template,
			String id);
	
	Future<Collection<String>> getProfileIds(String templateId);
	
}
