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

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.schedule.management.persistence.OgemaTimeSeriesPersistence;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.form.dropdown.DropdownOption;

// second generic argument has no meaning in this case
public class ScheduleType implements OgemaTimeSeriesPersistence<Schedule, Object> {
	
	private final ResourceAccess ra;
	
	public ScheduleType(ResourceAccess ra) {
		this.ra = ra;
	}

	@Override
	public String id() {
		return "schedule";
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Schedule";
	}
	
	@Override
	public String description(OgemaLocale locale) {
		return "OGEMA schedules, i.e. persistent time series.";
	}

	@Override
	public List<DropdownOption> getAllTimeseries(Object obj) {
		List<Schedule> schedules = ra.getResources(Schedule.class);
		List<DropdownOption> opts = schedules.stream()
				.map(schedule -> new DropdownOption(schedule.getPath(), schedule.getPath(), false))
				.collect(Collectors.toList());
		if (!opts.isEmpty())
			opts.get(0).select(true);
		return opts;
	}

	@Override
	public Schedule getTimeseries(Object obj, String id) {
		Resource res = ra.getResource(id);
		if (!(res instanceof Schedule))
			return null;
		return (Schedule) res;
	}

	// TODO parent resource type, like for log data?
	@Override
	public List<Object> getHighLevelOptions() {
		return null;
	}
	
	@Override
	public void store(ReadOnlyTimeSeries data, Schedule timeSeries, boolean replaceValues) throws IOException, IllegalArgumentException {
		Objects.requireNonNull(data);
		Objects.requireNonNull(timeSeries);
		SampledValue start = data.getNextValue(Long.MIN_VALUE);
		SampledValue end = data.getPreviousValue(Long.MAX_VALUE);
		if (start == null || end == null)
			return;
		if (replaceValues) {
			long endT = end.getTimestamp();
			if (endT < Long.MAX_VALUE)
				endT++;
			timeSeries.replaceValues(start.getTimestamp(), endT, data.getValues(start.getTimestamp()));
		}
		else
			timeSeries.addValues(data.getValues(Long.MIN_VALUE));
	}
	
	@Override
	public void clearCache() {
		// TODO Auto-generated method stub
	}

}
