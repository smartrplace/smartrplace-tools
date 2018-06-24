package org.smartrplace.tools.time.utils.templates;

import java.time.LocalTime;
import java.util.NavigableMap;

public interface DayTemplate<T> {

	NavigableMap<LocalTime, T> getDayValues();
	
}
