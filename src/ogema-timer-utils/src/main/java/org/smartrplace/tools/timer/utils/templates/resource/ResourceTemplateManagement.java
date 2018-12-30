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
package org.smartrplace.tools.timer.utils.templates.resource;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;

import org.ogema.core.model.ValueResource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.timer.utils.model.DayTemplateData;
import org.smartrplace.tools.timer.utils.model.DayTemplateProviderData;
import org.smartrplace.tools.timer.utils.model.TemplateData;
import org.smartrplace.tools.timer.utils.templates.DayTemplateProvider;
import org.smartrplace.tools.timer.utils.templates.TemplateManagement;

// TODO custom conversion
public class ResourceTemplateManagement<S extends ValueResource, T> implements TemplateManagement<T> {

	private final ResourceTemplateProvider<T> provider;
	private final DayTemplateProviderData data;
	private final Class<S> type;

	public ResourceTemplateManagement(final DayTemplateProviderData data, final Class<S> type) {
		this.data = Objects.requireNonNull(data);
		this.type = Objects.requireNonNull(type);
		if (!data.exists())
			data.create();
		this.provider = new ResourceTemplateProvider<>(data);
	}
	
	@Override
	public DayTemplateProvider<T> getPovider() {
		return provider;
	}

	@Override
	public void addValue(DayOfWeek day, LocalTime time, T value) {
		final DayTemplateData dayData = data.getDayTemplateData(day).create();
		addValue(dayData, time, value);
	}

	@Override
	public void addDefaultValue(LocalTime time, T value) {
		final DayTemplateData dayData = data.defaultDay().create();
		addValue(dayData, time, value);
	}

	@Override
	public void addValues(DayOfWeek day, Map<LocalTime, T> values) {
		final DayTemplateData dayData = data.getDayTemplateData(day).create();
		addValues(dayData, (Map<LocalTime,Object>) values);
	}

	@Override
	public void addValues(Map<DayOfWeek, Map<LocalTime, T>> values) {
		values.entrySet().forEach(entry -> addValues(entry.getKey(), entry.getValue()));
	}

	@Override
	public void setValues(DayOfWeek day, Map<LocalTime, T> values) {
		final DayTemplateData dayData = data.getDayTemplateData(day).create();
		setValues(dayData, (Map<LocalTime,Object>) values);
	}

	@Override
	public void setValues(Map<DayOfWeek, Map<LocalTime, T>> values) {
		values.entrySet().forEach(entry -> setValues(entry.getKey(), entry.getValue()));
	}

	@Override
	public void addDefaultValues(Map<LocalTime, T> values) {
		final DayTemplateData dayData = data.defaultDay().create();
		addValues(dayData, (Map<LocalTime,Object>) values);
	}

	@Override
	public void setDefaultValues(Map<LocalTime, T> values) {
		final DayTemplateData dayData = data.defaultDay().create();
		setValues(dayData, (Map<LocalTime,Object>) values);
	}

	@Override
	public void deleteValue(DayOfWeek day, LocalTime time) {
		final DayTemplateData dayData = data.getDayTemplateData(day);
		if (dayData.exists())
			deleteValue(dayData, time);
	}

	@Override
	public void deleteDefaultValue(LocalTime time) {
		final DayTemplateData dayData = data.defaultDay();
		if (dayData.exists())
			deleteValue(dayData, time);
	}

	@Override
	public void clear(DayOfWeek day) {
		data.getDayTemplateData(day).data().delete();
	}

	@Override
	public void clearDefault() {
		data.defaultDay().data().delete();
	}
	
	private TemplateData addData(final DayTemplateData dayData) {
		final TemplateData td = dayData.data().add();
		td.time().create();
		td.addDecorator("value", type); // we need to specialize the "value" subresource to the demanded type
		td.time().addValueListener(provider);
		td.value().addValueListener(provider);
		td.addStructureListener(provider);
		return td;
	}
	
	private void deleteData(final TemplateData td) {
		try {
			td.removeStructureListener(provider);
			td.time().removeValueListener(provider);
			td.value().removeValueListener(provider);
			td.delete();
		} catch (Exception e) {
			LoggerFactory.getLogger(ResourceTemplateManagement.class).warn("Exception deleting data",e);
		}
	}

	private void addValue(final DayTemplateData dayData, final LocalTime time, final Object value) {
		final long millis = time.toNanoOfDay() / 1000000;
		dayData.data().create();
		final TemplateData entry = dayData.data().getAllElements().stream()
			.filter(el -> el.time().getValue() == millis)
			.findAny().orElseGet(() -> addData(dayData));
		entry.time().setValue(millis);
		ValueResourceUtils.setValue(entry.value(), value);
		dayData.activate(true);
	}
	
	private void addValues(final DayTemplateData dayData, final Map<LocalTime, Object> values) {
		values.entrySet().stream().forEach(entry -> addValue(dayData, entry.getKey(), entry.getValue()));
		dayData.activate(true);
	}
	
	private void setValues(final DayTemplateData dayData, final Map<LocalTime, Object> values) {
		dayData.data().delete();
		addValues(dayData, values);
		dayData.activate(true);
	}
	
	private void deleteValue(final DayTemplateData dayData, final LocalTime time) {
		final long millis = time.toNanoOfDay() / 1000000;
		dayData.data().getAllElements().stream()
			.filter(el -> el.time().getValue() == millis)
			.forEach(el -> deleteData(el));
	}
	
}
