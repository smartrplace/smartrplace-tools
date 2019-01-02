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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NavigableMap;

import org.smartrplace.tools.timer.utils.templates.DayTemplate;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;

class TemplateUtils {

	/**
	 * Determine the next template value, with timestamp <= start time
	 * @param start
	 * @param provider
	 * @param zone
	 * @return
	 */
	static <T> Map.Entry<ZonedDateTime, T> getPreviousTemplateValue(final ZonedDateTime start, final DayTemplateProvider<T> provider, final ZoneId zone) {
		final LocalDate date = start.toLocalDate();
		ZonedDateTime next = start;
		DayTemplate<T> template = provider.getTemplate(date, zone);
		NavigableMap<LocalTime, T> data = template != null ? template.getDayValues().headMap(start.toLocalTime(), true) : null;
		int cnt = 0;
		while (cnt++ < 7 && (data == null || data.isEmpty())) {
			next = next.minusDays(1);
			template = provider.getTemplate(next.toLocalDate(), zone);
			data = template == null? null : template.getDayValues();
		}
		if (data == null || data.isEmpty()) { // no further execution
			return null;
		}
		final Map.Entry<LocalTime, T> entry = data.lastEntry();
		next = next.with(entry.getKey()); // TOOD test
		return new Entry<T>(next, entry.getValue());
	}
	
	
	/**
	 * Determine the next template value, with timestamp > start time
	 * @param start
	 * @return
	 */
	static <T> Map.Entry<ZonedDateTime, T> getNextTemplateValue(final ZonedDateTime start, final DayTemplateProvider<T> provider, final ZoneId zone) {
		final LocalDate date = start.toLocalDate();
		ZonedDateTime next = start;
		DayTemplate<T> template = provider.getTemplate(date, zone);
		NavigableMap<LocalTime, T> data = template != null ? template.getDayValues().tailMap(start.toLocalTime(), false) : null;
		int cnt = 0;
		while (cnt++ < 7 && (data == null || data.isEmpty())) {
			next = next.plusDays(1);
			template = provider.getTemplate(next.toLocalDate(), zone);
			data = template == null ? null : template.getDayValues();
		}
		if (data == null || data.isEmpty()) { // no further execution
			return null;
		}
		final Map.Entry<LocalTime, T> entry = data.firstEntry();
		next = next.with(entry.getKey()); // TOOD test
		return new Entry<T>(next, entry.getValue());
	}
	
	
	private static class Entry<T> implements Map.Entry<ZonedDateTime, T> {

		private final ZonedDateTime key;
		private final T value;
		
		Entry(ZonedDateTime key, T value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public ZonedDateTime getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public T setValue(T value) {
			throw new UnsupportedOperationException();
		}
		
		
		
	}
	
}
