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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;
import org.smartrplace.tools.timer.utils.templates.TemplateManagement;

public class SimpleTemplateManagement<T> implements TemplateManagement<T> {

	private final SimpleTemplateProvider<T> provider;
	
	public SimpleTemplateManagement() {
		this.provider = new SimpleTemplateProvider<>();
	}

	@Override
	public DayTemplateProvider<T> getPovider() {
		return provider;
	}

	@Override
	public void addValue(DayOfWeek day, LocalTime time, T value) {
		Objects.requireNonNull(time);
		Objects.requireNonNull(value);
		Objects.requireNonNull(day);
		final SimpleDayTemplate<T> template = provider.getTemplate(day, true);
		template.dataInternal.put(time, value);
		provider.touched();
	}

	@Override
	public void addDefaultValue(LocalTime time, T value) {
		Objects.requireNonNull(time);
		Objects.requireNonNull(value);
		final SimpleDayTemplate<T> template = provider.getDefaultTemplate(true);
		template.dataInternal.put(time, value);
		provider.touched();

	}

	@Override
	public void addValues(DayOfWeek day, Map<LocalTime, T> values) {
		Objects.requireNonNull(values);
		Objects.requireNonNull(day);
		if (values.isEmpty())
			return;
		final SimpleDayTemplate<T> template = provider.getTemplate(day, true);
		template.dataInternal.putAll(values);
		provider.touched();
		
	}

	@Override
	public void addValues(Map<DayOfWeek, Map<LocalTime, T>> values) {
		Objects.requireNonNull(values);
		if (values.isEmpty())
			return;
		values.entrySet().forEach(entry -> provider.getTemplate(entry.getKey(), true).dataInternal.putAll(entry.getValue()));
		provider.touched();
	}

	@Override
	public void setValues(DayOfWeek day, Map<LocalTime, T> values) {
		Objects.requireNonNull(values);
		Objects.requireNonNull(day);
		final SimpleDayTemplate<T> template = provider.getTemplate(day, true);
		// FIXME synchronize dataInternal? Avoid making intermediate steps visible
		template.dataInternal.clear();
		template.dataInternal.putAll(values);
		provider.touched();
		
	}

	@Override
	public void setValues(Map<DayOfWeek, Map<LocalTime, T>> values) {
		Objects.requireNonNull(values);
		if (values.isEmpty())
			return;
		values.entrySet().forEach(entry -> {
			final SimpleDayTemplate<T> template = provider.getTemplate(entry.getKey(), true);
			template.dataInternal.clear();
			template.dataInternal.putAll(entry.getValue());
		});
		provider.touched();
		
	}

	@Override
	public void addDefaultValues(Map<LocalTime, T> values) {
		Objects.requireNonNull(values);
		if (values.isEmpty())
			return;
		provider.getDefaultTemplate(true).dataInternal.putAll(values);
		provider.touched();
		
	}

	@Override
	public void setDefaultValues(Map<LocalTime, T> values) {
		Objects.requireNonNull(values);
		if (values.isEmpty())
			return;
		final SimpleDayTemplate<T> template = provider.getDefaultTemplate(true);
		template.dataInternal.clear();
		template.dataInternal.putAll(values);
		provider.touched();
	}

	@Override
	public void deleteValue(DayOfWeek day, LocalTime time) {
		Objects.requireNonNull(time);
		Objects.requireNonNull(day);
		final SimpleDayTemplate<T> template = provider.getTemplate(day, false);
		if (template != null && template.dataInternal.remove(time) != null)
			provider.touched();
		
	}

	@Override
	public void deleteDefaultValue(LocalTime time) {
		Objects.requireNonNull(time);
		final SimpleDayTemplate<T> template = provider.getDefaultTemplate(false);
		if (template != null && template.dataInternal.remove(time) != null)
			provider.touched();
	}

	@Override
	public void clear(DayOfWeek day) {
		Objects.requireNonNull(day);
		final SimpleDayTemplate<T> template = provider.getTemplate(day, false);
		if (template != null && !template.dataInternal.isEmpty()) {
			template.dataInternal.clear();
			provider.touched();
		}
	}

	@Override
	public void clearDefault() {
		final SimpleDayTemplate<T> template = provider.getDefaultTemplate(false);
		if (template != null && !template.dataInternal.isEmpty()) {
			template.dataInternal.clear();
			provider.touched();
		}
	}
	
}
