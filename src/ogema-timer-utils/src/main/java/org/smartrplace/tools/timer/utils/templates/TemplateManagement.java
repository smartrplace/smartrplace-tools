package org.smartrplace.tools.timer.utils.templates;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

/**
 * Edit template values of a {@link DayTemplateProvider}.
 * 
 * @param <T>
 */
public interface TemplateManagement<T> {

	DayTemplateProvider<T> getPovider();
	
	void addValue(DayOfWeek day, LocalTime time, T value);
	void addDefaultValue(LocalTime time, T value);
	
	void addValues(DayOfWeek day, Map<LocalTime, T> values);
	void addValues(Map<DayOfWeek, Map<LocalTime, T>> values);
	void setValues(DayOfWeek day, Map<LocalTime, T> values);
	void setValues(Map<DayOfWeek, Map<LocalTime, T>> values);
	
	void addDefaultValues(Map<LocalTime, T> values);
	void setDefaultValues(Map<LocalTime, T> values);
	
	void deleteValue(DayOfWeek day, LocalTime time);
	void deleteDefaultValue(LocalTime time);
	
	void clear(DayOfWeek day);
	void clearDefault();
	
}
