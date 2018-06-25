package org.smartrplace.tools.timer.utils.templates;

import java.time.LocalTime;
import java.util.Collections;
import java.util.NavigableMap;

public interface DayTemplate<T> {

	@SuppressWarnings({ "rawtypes" })
	public static final DayTemplate EMPTY_TEMPLATE = new DayTemplate() {

		@Override
		public NavigableMap getDayValues() {
			return Collections.emptyNavigableMap();
		}
	};

	NavigableMap<LocalTime, T> getDayValues();
	
}
