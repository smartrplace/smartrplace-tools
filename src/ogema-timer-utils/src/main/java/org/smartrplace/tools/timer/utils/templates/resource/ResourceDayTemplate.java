package org.smartrplace.tools.timer.utils.templates.resource;

import java.time.LocalTime;
import java.util.NavigableMap;

import org.smartrplace.tools.timer.utils.model.DayTemplateData;
import org.smartrplace.tools.timer.utils.templates.DayTemplate;

class ResourceDayTemplate<T> implements DayTemplate<T> {
	
	private final DayTemplateData data;
	
	ResourceDayTemplate(DayTemplateData data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public NavigableMap<LocalTime, T> getDayValues() {
		return (NavigableMap<LocalTime, T>) data.getValuesAsMap();
	}
	
}
