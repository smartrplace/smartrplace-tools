package org.smartrplace.tools.timer.utils.templates.resource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ogema.core.model.ValueResource;
import org.ogema.core.resourcemanager.ResourceStructureEvent;
import org.ogema.core.resourcemanager.ResourceStructureListener;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.tools.timer.utils.model.DayTemplateData;
import org.smartrplace.tools.timer.utils.model.DayTemplateProviderData;
import org.smartrplace.tools.timer.utils.templates.DayTemplate;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;

// TODO template resource changes
class ResourceTemplateProvider<T> implements DayTemplateProvider<T>, ResourceValueListener<ValueResource>, ResourceStructureListener {
	
	private final Set<TemplateListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>(4));
	private final DayTemplateProviderData data;
	// TODO shared between providers?
	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	
	ResourceTemplateProvider(DayTemplateProviderData data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DayTemplate<T> getTemplate(LocalDate day, ZoneId timeZone) {
		final DayOfWeek dow = day.getDayOfWeek();
		DayTemplateData dayData  = data.getDayTemplateData(dow);
		if (!dayData.isActive())
			dayData = data.defaultDay();
		if (!dayData.isActive())
			return DayTemplate.EMPTY_TEMPLATE;
		return new ResourceDayTemplate<T>(dayData);
	}

	@Override
	public void addTemplateChangeListener(TemplateListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeTemplateChangeListener(TemplateListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void resourceChanged(ValueResource resource) {
		listeners.forEach(l -> exec.submit(() -> l.templateChanged(ResourceTemplateProvider.this)));
	}

	@Override
	public void resourceStructureChanged(ResourceStructureEvent event) {
		listeners.forEach(l -> exec.submit(() -> l.templateChanged(ResourceTemplateProvider.this)));
	}
	
}
