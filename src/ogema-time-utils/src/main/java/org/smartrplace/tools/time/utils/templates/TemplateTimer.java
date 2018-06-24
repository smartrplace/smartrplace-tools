package org.smartrplace.tools.time.utils.templates;

import java.time.ZonedDateTime;
import java.util.Map;

import org.ogema.core.application.Timer;

public interface TemplateTimer<T> extends Timer {
	
	/**
	 * Get the currently active value, with timestamp smaller or equal to now.
	 * @return
	 */
	Map.Entry<ZonedDateTime, T> getPreviousValue();
	/**
	 * Get the following value, with timestamp strictly greater than now.
	 * @return
	 */
	Map.Entry<ZonedDateTime, T> getNextValue();

	void addTemplateTimerListener(TemplateTimerListener<T> listener);
	void removeTemplateTimerListener(TemplateTimerListener<T> listener);
	
	public static interface TemplateTimerListener<S> {
		
		void timerElapsed(TemplateTimer<S> timer, S value);
		
	}
	
}
