package org.smartrplace.tools.profiles.utils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.Profile;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ProfileImpl implements Profile {
	
	private final String id;
	private final String templateId;
	private final ProfileTemplate template;
	private final Map<DataPoint, Object> primary;
	private final Map<DataPoint, Object> context;
	private final Map<Long, State> stateEndTimes;
	
	public ProfileImpl(String id, Map<DataPoint, Object> primary, Map<DataPoint, Object> context, Map<Long, State> stateEndTimes, ProfileTemplate template) {
		this.id = Objects.requireNonNull(id);
		this.primary = Collections.unmodifiableMap(primary);
		this.context = Collections.unmodifiableMap(context);
		this.template = Objects.requireNonNull(template);
		this.stateEndTimes = Collections.unmodifiableMap(stateEndTimes);
		this.templateId = template.id();
	}

	@Override
	public Object getPrimaryData(DataPoint dp) {
		return primary.get(dp);
	}

	@Override
	public Object getContextData(DataPoint dp) {
		return context.get(dp);
	}
	
	@Override
	public Map<DataPoint, Object> getPrimaryData() {
		return primary;
	}
	
	@Override
	public Map<DataPoint, Object> getContextData() {
		return context;
	}
	
	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return template.label(locale);
	}
	
	@Override
	public String description(OgemaLocale locale) {
		return template.description(locale);
	}
	
	@Override
	public String templateId() {
		return templateId;
	}
	
	@Override
	public Map<Long, State> stateEndTimes() {
		return stateEndTimes;
	}
	
	@Override
	public String toString() {
		return "ProfileImpl[" + id() + "]";
	}
	
}
