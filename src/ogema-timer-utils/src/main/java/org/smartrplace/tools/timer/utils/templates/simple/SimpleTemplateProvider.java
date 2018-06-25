package org.smartrplace.tools.timer.utils.templates.simple;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.smartrplace.tools.timer.utils.templates.DayTemplate;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;

/**
 * Non-persistent implementation of {@link DayTemplate}.
 */
// TODO holidays
class SimpleTemplateProvider<T> implements DayTemplateProvider<T> {
	
	// TODO shared between providers?
	private final ExecutorService exec = Executors.newSingleThreadExecutor();
	// synchronized on this
	private volatile SimpleDayTemplate<T> defaultTemplate;
	// synchronized on itself
	private Map<DayOfWeek, SimpleDayTemplate<T>> dayTemplates = Collections.synchronizedMap(new EnumMap<>(DayOfWeek.class));
	private final Set<TemplateListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	@SuppressWarnings("unchecked")
	@Override
	public DayTemplate<T> getTemplate(LocalDate day, ZoneId timeZone) {
		final SimpleDayTemplate<T> template = dayTemplates.getOrDefault(day.getDayOfWeek(), defaultTemplate);
		return template != null ? template: SimpleDayTemplate.EMPTY_TEMPLATE;
	}
	
	@Override
	public void addTemplateChangeListener(TemplateListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeTemplateChangeListener(TemplateListener listener) {
		listeners.remove(listener);
	}

	SimpleDayTemplate<T> getDefaultTemplate(boolean create) {
		SimpleDayTemplate<T> template = this.defaultTemplate;
		if (create && template == null) {
			synchronized (this) {
				template = this.defaultTemplate;
				if (template == null) {
					template = new SimpleDayTemplate<>();
					this.defaultTemplate = template;
				}
			}
		}
		return template;
	}
	
	SimpleDayTemplate<T> getTemplate(DayOfWeek day, boolean create) {
		if (create)
			return dayTemplates.computeIfAbsent(day, (d) -> new SimpleDayTemplate<>());
		else 
			return dayTemplates.get(day);
	}
	
	void touched() {
		listeners.forEach(l -> exec.execute(new TemplateChangeEvent(this, l)));
	}

	private static class TemplateChangeEvent implements Runnable {
		
		private final SimpleTemplateProvider<?> provider;
		private final TemplateListener listener;
		
		TemplateChangeEvent(SimpleTemplateProvider<?> provider, TemplateListener listener) {
			this.provider = provider;
			this.listener = listener;
		}
		
		@Override
		public void run() {
			listener.templateChanged(provider);
		}
		
	}
	
}
