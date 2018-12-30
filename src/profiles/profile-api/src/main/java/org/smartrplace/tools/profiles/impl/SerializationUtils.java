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
package org.smartrplace.tools.profiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.timeseries.implementations.FloatTreeTimeSeries;
import org.ogema.tools.timeseries.implementations.TreeTimeSeries;
import org.osgi.service.component.ComponentServiceObjects;
import org.smartrplace.tools.profiles.DataPoint;
import org.smartrplace.tools.profiles.ProfileTemplate;
import org.smartrplace.tools.profiles.State;
import org.smartrplace.tools.profiles.utils.ProfileImpl;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class SerializationUtils {
	
	private static final ObjectMapper mapper = new ObjectMapper()
			.setVisibility(PropertyAccessor.ALL, Visibility.NONE)
			.setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
			.setSerializationInclusion(Include.NON_NULL);
	
	private static final ObjectReader reader = mapper.readerFor(ProfileDTO.class);
	private static final ObjectWriter writer = mapper.writerFor(ProfileDTO.class)
			.withDefaultPrettyPrinter();
	
	public static void write(final OutputStream out, final ProfileImpl profile) throws JsonGenerationException, JsonMappingException, IOException {
		final ProfileDTO dto = serialize(profile);
		writer.writeValue(out, dto);
	}
	
	public static void write(final Writer out, final ProfileImpl profile) throws JsonGenerationException, JsonMappingException, IOException {
		final ProfileDTO dto = serialize(profile);
		writer.writeValue(out, dto);
	}
	
	public static ProfileImpl read(final InputStream in, final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates) throws JsonProcessingException, IOException {
		final ProfileDTO dto = reader.readValue(in);
		return deserialize(dto, templates);
	}
	
	public static ProfileImpl read(final Reader in, final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates) throws JsonProcessingException, IOException {
		final ProfileDTO dto = reader.readValue(in);
		return deserialize(dto, templates);
	}
	
	
	/**
	 * @param dto
	 * @param templates
	 * @return
	 * @throws NullPointerException if {@link ProfileDTO#templateId} is null
	 * @throws IllegalStateException if the required {@link ProfileTemplate} is not available
	 */
	public static ProfileImpl deserialize(ProfileDTO dto, final ConcurrentMap<String, ComponentServiceObjects<ProfileTemplate>> templates) {
		final String templateId = dto.templateId;
		if (templateId == null)
			throw new NullPointerException("No template id provided");
		final ComponentServiceObjects<ProfileTemplate> service = templates.get(templateId);
		if (service == null) {
			throw new IllegalStateException("Template provider for id " + templateId + " not available");
		}
		final ProfileTemplate template = service.getService();
		try {
			final List<DataPoint> primary = template.primaryData();
			final List<DataPoint> context = template.contextData();
			final List<DataPoint> derived = template.derivedData();
			final Map<DataPoint, Object> primaryResults = new LinkedHashMap<>();
			final Map<DataPoint, Object> contextResults = new LinkedHashMap<>();
			final Map<DataPoint, Object> derivedResults = new LinkedHashMap<>();
			final List<State> states = template.states();
			final Map<Long, State> endTimes = dto.stateEndTimes.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> 
					states.stream().filter(state -> entry.getValue().equals(state.id())).findAny().orElseThrow(() -> new IllegalArgumentException("Unknown state " + entry.getValue()))));
			addPoints(primary, dto, primaryResults);
			addPoints(context, dto, contextResults);
			addPoints(derived, dto, derivedResults);
			final ProfileImpl profile = new ProfileImpl(dto.id, primaryResults, contextResults, derivedResults, endTimes, template);
			return profile;
		} finally {
			service.ungetService(template);
		}
	}
	
	private static void addPoints(final List<DataPoint> points, final ProfileDTO dto, final Map<DataPoint, Object> results) {
		if (points == null)
			return;
		for (DataPoint dp : points) {
			final String key = dp.id();
			final Object value = getValue(key, dto);
			if (value == null)
				continue;
			results.put(dp, value);
		}
	}
	
	private static Object getValue(final String key, final ProfileDTO dto) {
		if (key == null)
			return null;
		if (dto.stringValues != null && dto.stringValues.containsKey(key))
			return dto.stringValues.get(key);
		if (dto.numericalValues != null && dto.numericalValues.containsKey(key))
			return dto.numericalValues.get(key);
		if (dto.timeseriesValues != null && dto.timeseriesValues.containsKey(key)) {
			final TimeseriesDTO tsDto = dto.timeseriesValues.get(key);
			return deserialize(tsDto);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static ProfileDTO serialize(ProfileImpl profile) {
		final ProfileDTO dto = new ProfileDTO();
		dto.id = profile.id();
		dto.templateId = profile.templateId();
		try {
			final Field primaryField = ProfileImpl.class.getDeclaredField("primary");
			primaryField.setAccessible(true);
			final Map<DataPoint, Object> primary = (Map<DataPoint, Object>) primaryField.get(profile);
			final Field contextField = ProfileImpl.class.getDeclaredField("context");
			contextField.setAccessible(true);
			final Map<DataPoint, Object> context = (Map<DataPoint, Object>) contextField.get(profile);
			final Field derivedField = ProfileImpl.class.getDeclaredField("derived");
			derivedField.setAccessible(true);
			final Map<DataPoint, Object> derived = (Map<DataPoint, Object>) derivedField.get(profile);
			addValues(dto, context);
			addValues(dto, primary);
			addValues(dto, derived);
		} catch (Exception e) {
			throw new RuntimeException("Unexpected exception",e);
		}
		dto.stateEndTimes = profile.stateEndTimes().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().id()));
		return dto;
	}
	
	private static TimeseriesDTO serialize(final ReadOnlyTimeSeries ts) {
		final TimeseriesDTO dto = new TimeseriesDTO();
		final int size = Math.max(4, ts.size());
		dto.timestamps = new ArrayList<>(size);
		dto.values = new ArrayList<>(size);
		final Iterator<SampledValue> it = ts.iterator();
		while (it.hasNext()) {
			final SampledValue sv = it.next();
			if (sv.getQuality() == Quality.BAD)
				continue;
			dto.timestamps.add(sv.getTimestamp());
			dto.values.add(sv.getValue().getFloatValue());
		}
		return dto;
	}
	
	private static ReadOnlyTimeSeries deserialize(final TimeseriesDTO dto) {
		final TreeTimeSeries ts = new FloatTreeTimeSeries();
		if (dto.values == null || dto.timestamps == null)
			throw new NullPointerException("Timestamps or values are null");
		if (dto.values.size() != dto.timestamps.size())
			throw new IllegalStateException("Nr of timestamps does not equal nr of values");
		final Iterator<Long> tIt = dto.timestamps.iterator();
		final Iterator<Float> vIt = dto.values.iterator();
		final List<SampledValue> values = new ArrayList<>(dto.timestamps.size());
		while (tIt.hasNext()) {
			values.add(new SampledValue(new FloatValue(vIt.next()), tIt.next(), Quality.GOOD));
		}
		ts.addValues(values);
		return ts;
	}
	
	private static void addValues(final ProfileDTO dto, final Map<DataPoint, Object> values) {
		for (Map.Entry<DataPoint, Object> entry : values.entrySet()) {
			final Object v = entry.getValue();
			if (v instanceof String) {
				if (dto.stringValues == null)
					dto.stringValues = new HashMap<>(4);
				dto.stringValues.put(entry.getKey().id(), (String) v);
			} else if (v instanceof Number) {
				if (dto.numericalValues == null)
					dto.numericalValues = new HashMap<>(4);
				dto.numericalValues.put(entry.getKey().id(), (Number) v);
			} else if (v instanceof ReadOnlyTimeSeries) {
				if (dto.timeseriesValues == null)
					dto.timeseriesValues = new HashMap<>(8);
				dto.timeseriesValues.put(entry.getKey().id(), serialize((ReadOnlyTimeSeries) v));
			}
		}
	}
	
}
