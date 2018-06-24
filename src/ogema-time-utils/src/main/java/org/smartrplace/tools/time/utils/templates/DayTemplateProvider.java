package org.smartrplace.tools.time.utils.templates;

import java.time.LocalDate;
import java.time.ZoneId;

public interface DayTemplateProvider<T> {

	DayTemplate<T> getTemplate(LocalDate day, ZoneId timeZone);
	
	void addTemplateChangeListener(TemplateListener listener);
	void removeTemplateChangeListener(TemplateListener listener);

	public static interface TemplateListener {
		
		void templateChanged(DayTemplateProvider<?> provider);
		
	}
	
}
