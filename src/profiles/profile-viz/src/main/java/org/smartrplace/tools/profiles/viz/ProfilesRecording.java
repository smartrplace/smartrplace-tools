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
package org.smartrplace.tools.profiles.viz;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ogema.core.application.ApplicationManager;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.profiles.ProfileGeneration;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.prefs.ProfilePreferences;

import de.iwes.widgets.api.widgets.LazyWidgetPage;
import de.iwes.widgets.api.widgets.WidgetPage;

@Component(
		service = LazyWidgetPage.class,
		property = {
				LazyWidgetPage.BASE_URL + "=/org/smartrplace/profiles",
				LazyWidgetPage.RELATIVE_URL + "=index.html",
				LazyWidgetPage.START_PAGE + "=true",
				LazyWidgetPage.MENU_ENTRY + "=Profiles generation"
		}
)
public class ProfilesRecording implements LazyWidgetPage {
	
	private final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates = new ConcurrentHashMap<>(8);
	
	@Reference(service=ProfileGeneration.class)
	private ComponentServiceObjects<ProfileGeneration> generation;
	
	@Reference(service=ProfilePreferences.class)
	private ComponentServiceObjects<ProfilePreferences> preferences;

	@Reference(
			service=ProfileTemplate.class,
			cardinality=ReferenceCardinality.MULTIPLE,
			policy=ReferencePolicy.DYNAMIC,
			policyOption=ReferencePolicyOption.GREEDY,
			bind="addTemplate",
			unbind="removeTemplate"
	)
	protected void addTemplate(final ComponentServiceObjects<ProfileTemplate> templateRef) {
		final String id = getId(templateRef);
		if (id == null)
			throw new ComponentException("Profile template without id: " + templateRef);
		final ComponentServiceObjects<ProfileTemplate> old = templates.put(id, templateRef);
		if (old != null) {
			final ProfileTemplate oldTemplate = old.getService();
			final ProfileTemplate newTemplate = templateRef.getService();
			try {
				LoggerFactory.getLogger(getClass()).warn("Duplicate profile template id {}: {}, {}", id, oldTemplate, newTemplate);
			} finally {
				templateRef.ungetService(newTemplate);
				old.ungetService(oldTemplate);
			}
		}
	}
	
	protected void removeTemplate(final ComponentServiceObjects<ProfileTemplate> templateRef) {
		final String id = getId(templateRef);
		if (id == null)
			return;
		templates.remove(id, templateRef);
	}
	
	private static String getId(final ComponentServiceObjects<ProfileTemplate> service) {
		Object id0 = service.getServiceReference().getProperty(ProfileTemplate.ID_PROPERTY);
		String id = id0 != null ? id0.toString() : null;
		if (id == null) {
			final ProfileTemplate template = service.getService();
			try {
				id = template.id();
			} finally {
				service.ungetService(template);
			}
		} 
		return id;
	}
	
	@Override
	public void init(ApplicationManager appMan, WidgetPage<?> page) {
		new RecordingPageInit(page, appMan, generation, templates, preferences);
	}

}
