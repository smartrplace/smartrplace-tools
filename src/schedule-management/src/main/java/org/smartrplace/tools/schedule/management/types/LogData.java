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
package org.smartrplace.tools.schedule.management.types;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.PhysicalUnitResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.form.dropdown.DropdownOption;

public class LogData implements OgemaDataSource<RecordedData, Class<? extends SingleValueResource>> {

	private final ResourceAccess ra;
	private final Cache<Class<? extends SingleValueResource>, List<SingleValueResource>> cache = CacheBuilder.newBuilder().softValues().build();
	private volatile SoftReference<List<Class<? extends SingleValueResource>>> types = new SoftReference<List<Class<? extends SingleValueResource>>>(null);
	
	public LogData(ResourceAccess ra) {
		this.ra = ra;
	}
	
	@Override
	public String id() {
		return "logdata";
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Log data";
	}

	@Override
	public String description(OgemaLocale locale) {
		return "Log data for OGEMA resources";
	}

	// potentially expensive -> cache
	@Override
	public List<DropdownOption> getAllTimeseries(Class<? extends SingleValueResource> type) {
		if (type == null)
			type = SingleValueResource.class;
		List<SingleValueResource> loggedResources = cache.getIfPresent(type);
		if (loggedResources == null) {
			loggedResources = new ArrayList<>();
			for (SingleValueResource svr : ra.getResources(SingleValueResource.class)) {
				if (LoggingUtils.isLoggingEnabled(svr)) 
					loggedResources.add(svr);
			}
			cache.put(type, loggedResources);
		}
		List<DropdownOption> opts = loggedResources.stream()
				.map(resource -> new DropdownOption(resource.getPath(), resource.getPath(), false))
				.collect(Collectors.toList());
		if (!opts.isEmpty())
			opts.get(0).select(true);
		return opts;
	}

	@Override
	public RecordedData getTimeseries(Class<? extends SingleValueResource> type, String id) {
		Resource res = ra.getResource(id);
		if (!(res instanceof SingleValueResource))
			return null;
		if (type != null && !type.isAssignableFrom(res.getResourceType()))
			return null;
		return LoggingUtils.getHistoricalData((SingleValueResource) res);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Class<? extends SingleValueResource>> getHighLevelOptions() {
		List<Class<? extends SingleValueResource>> types = this.types.get();
		if (types == null) {
			types = new ArrayList<>();
			types.add(SingleValueResource.class);
			types.add(FloatResource.class);
			types.add(IntegerResource.class);
			types.add(BooleanResource.class);
			types.add(TimeResource.class);
			types.add(PhysicalUnitResource.class);
			List<PhysicalUnitResource> purs = ra.getResources(PhysicalUnitResource.class);
			Class<? extends SingleValueResource> type;
			for (PhysicalUnitResource pur : purs) { 
				type = (Class<? extends SingleValueResource>) pur.getResourceType();
				if (!types.contains(type))
					types.add(type);
			}
			this.types = new SoftReference<List<Class<? extends SingleValueResource>>>(types);
		}
		return types;
	}

	@Override
	public void clearCache() {
		cache.asMap().clear();
		types = new SoftReference<List<Class<? extends SingleValueResource>>>(null);
	}
	
}
