package org.smartrplace.tools.timer.utils.templates.simple;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.smartrplace.tools.timer.utils.DelegatingTimer;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;
import org.smartrplace.tools.timer.utils.templates.TemplateTimer;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider.TemplateListener;

public class SimpleTemplateTimer<T> extends DelegatingTimer implements TemplateTimer<T> {
	
	final ApplicationManager appMan;
	final DayTemplateProvider<T> templateProvider;
	private final TemplateListener templateChangeListener;
	final ZoneId timeZone;
	final Set<TemplateTimerListener<T>> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public SimpleTemplateTimer(final ApplicationManager appMan, final DayTemplateProvider<T> templateProvider, 
			final TimerListener listener, final ZoneId timeZone) {
		super(Objects.requireNonNull(appMan).createTimer(Long.MAX_VALUE));
		this.appMan = appMan;
		this.templateProvider = Objects.requireNonNull(templateProvider);
		this.timeZone = timeZone != null ? timeZone : ZoneId.systemDefault();
		if (listener != null)
			baseTimer.addListener(listener);
		final SettingsListener<T> settingsListener = new SettingsListener<>(this);
		baseTimer.addListener(settingsListener);
		final Callable<Void> settingsTask = () -> {
			settingsListener.timerElapsed(baseTimer);
			return (Void) null;
		};
		appMan.submitEvent(settingsTask);
		// when the template changes, we need to recalculate the next execution time
		this.templateChangeListener = (provider) -> appMan.submitEvent(settingsTask); 
		templateProvider.addTemplateChangeListener(templateChangeListener);
	}
	
	@Override
	public void destroy() {
		try {
			templateProvider.removeTemplateChangeListener(templateChangeListener);
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.destroy();
	}
	
	@Override
	public List<TimerListener> getListeners() {
		return super.getListeners().stream()
				.filter(l -> (!(l instanceof SettingsListener)))
				.collect(Collectors.toList());
	}
	

	@Override
	public void addTemplateTimerListener(TemplateTimerListener<T> listener) {
		listeners.add(listener);
	}
	
	@Override
	public void removeTemplateTimerListener(TemplateTimerListener<T> listener) {
		listeners.remove(listener);
	}
	
	@Override
	public Entry<ZonedDateTime, T> getNextValue() {
		final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(appMan.getFrameworkTime()), timeZone);
		return TemplateUtils.getNextTemplateValue(now, templateProvider, timeZone);
	}
	
	@Override
	public Entry<ZonedDateTime, T> getPreviousValue() {
		final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(appMan.getFrameworkTime()), timeZone);
		return TemplateUtils.getPreviousTemplateValue(now, templateProvider, timeZone);
	}
	
	private static class SettingsListener<S> implements TimerListener {

		private final SimpleTemplateTimer<S> master;
		
		SettingsListener(SimpleTemplateTimer<S> master) {
			this.master = master;
		}

		@Override
		public void timerElapsed(Timer timer) {
			timer.stop();
			// dispatch event to template timer listeners
			// determine current template value
			final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timer.getExecutionTime()), master.timeZone);
			final Map.Entry<ZonedDateTime, S> previous = TemplateUtils.getPreviousTemplateValue(now, master.templateProvider, master.timeZone); 
			if (previous != null) {
				master.listeners.forEach(l -> master.appMan.submitEvent(new TimerEvent<S>(master, l, previous.getValue())));
			}
			// determine next timer run time
			final Map.Entry<ZonedDateTime, S> entry = TemplateUtils.getNextTemplateValue(now, master.templateProvider, master.timeZone);
			if (entry == null)
				return;
 			timer.setTimingInterval(entry.getKey().toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
			timer.resume();
		}
		
	}
	
	static class TimerEvent<S> implements Callable<Void> {
		
		private final SimpleTemplateTimer<S> timer;
		private final TemplateTimerListener<S> listener;
		private final S object;

		TimerEvent(SimpleTemplateTimer<S> timer, TemplateTimerListener<S> listener, S object) {
			this.timer = timer;
			this.listener = listener;
			this.object = object;
		}

		@Override
		public Void call() throws Exception {
			listener.timerElapsed(timer, object);
			return null;
		}
		
	}
	
	
}

