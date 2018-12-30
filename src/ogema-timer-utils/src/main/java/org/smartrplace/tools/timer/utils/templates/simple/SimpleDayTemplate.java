/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
