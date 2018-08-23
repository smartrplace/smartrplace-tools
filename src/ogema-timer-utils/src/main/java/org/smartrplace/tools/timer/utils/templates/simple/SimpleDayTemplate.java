package org.smartrplace.tools.timer.utils.templates.simple;

import java.time.LocalTime;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.smartrplace.tools.timer.utils.templates.DayTemplate;

/**
 * A non-persistent {@link DayTemplate}, based on a {@link ConcurrentNavigableMap}.
 */
class SimpleDayTemplate<T> implements DayTemplate<T> {
	
	final NavigableMap<LocalTime, T> dataInternal;
	private final NavigableMap<LocalTime, T> dataExternal;

	SimpleDayTemplate() {
		this(new ConcurrentSkipListMap<>());
	}
	
	/**
	 * @param data
	 * 		a concurrent navigable map, such as a {@link ConcurrentSkipListMap}.
	 */
	SimpleDayTemplate(final NavigableMap<LocalTime, T> data) {
		this.dataInternal = data;
		this.dataExternal  = Collections.unmodifiableNavigableMap(dataInternal);
	}
	
	@Override
	public NavigableMap<LocalTime, T> getDayValues() {
		return dataExternal;
	}

}