/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
	private final Map<DataPoint, Object> derived;
	private final Map<Long, State> stateEndTimes;
	
	public ProfileImpl(String id, Map<DataPoint, Object> primary, Map<DataPoint, Object> context, Map<DataPoint, Object> derived, 
			Map<Long, State> stateEndTimes, ProfileTemplate template) {
		this.id = Objects.requireNonNull(id);
		this.primary = Collections.unmodifiableMap(primary);
		this.context = context == null ? Collections.emptyMap() : Collections.unmodifiableMap(context);
		this.derived = derived == null ? Collections.emptyMap() : Collections.unmodifiableMap(derived);
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
	public Object getDerivedData(DataPoint dp) {
		return derived.get(dp);
	}
	
	@Override
	public Map<DataPoint, Object> getDerivedData() {
		return derived;
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
