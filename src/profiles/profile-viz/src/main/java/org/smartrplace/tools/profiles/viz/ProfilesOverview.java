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

import org.ogema.core.application.ApplicationManager;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.smartrplace.tools.profiles.ProfileGeneration;
import de.iwes.widgets.api.widgets.LazyWidgetPage;
import de.iwes.widgets.api.widgets.WidgetPage;

@Component(
		service = LazyWidgetPage.class,
		property = {
				LazyWidgetPage.BASE_URL + "=/org/smartrplace/profiles",
				LazyWidgetPage.RELATIVE_URL + "=overview.html",
				LazyWidgetPage.MENU_ENTRY + "=Profiles overview"
		}
)
public class ProfilesOverview implements LazyWidgetPage {
	
	@Reference(service=ProfileGeneration.class)
	private ComponentServiceObjects<ProfileGeneration> generation;
	
	@Override
	public void init(ApplicationManager appMan, WidgetPage<?> page) {
		new OverviewPageInit(page, appMan, generation);
	}

}
