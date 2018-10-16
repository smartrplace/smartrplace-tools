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
