/**
 * Copyright 2018 Smartrplace UG
 *
 * Schedule management is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Schedule management is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
