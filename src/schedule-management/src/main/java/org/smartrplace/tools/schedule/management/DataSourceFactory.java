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
package org.smartrplace.tools.schedule.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;
import org.smartrplace.tools.schedule.management.persistence.OgemaTimeSeriesPersistence;
import org.smartrplace.tools.schedule.management.types.LogData;
import org.smartrplace.tools.schedule.management.types.ScheduleType;

class DataSourceFactory {

	final ScheduleManagementApp app;
	final ScheduleType scheduleType;
	final LogData logData;
	private final Set<OgemaDataSource<?, ?>> sources = Collections.synchronizedSet(new HashSet<>());
	private final Set<OgemaTimeSeriesPersistence<?, ?>> targets = Collections.synchronizedSet(new HashSet<>());
	
	DataSourceFactory(ScheduleManagementApp app) {
		this.app = app;
		this.scheduleType = new ScheduleType(app.am.getResourceAccess());
		this.logData = new LogData(app.am.getResourceAccess());
	}
	
	public List<OgemaDataSource<?,?>> getSources() {
		final List<OgemaDataSource<?, ?>> sources;
		synchronized (this.sources) {
			sources = new ArrayList<>(this.sources);
		}
		sources.add(scheduleType);
		sources.add(logData);
		return sources;
	}
	
	public List<OgemaTimeSeriesPersistence<?, ?>> getTargets() {
		final List<OgemaTimeSeriesPersistence<?, ?>> targets;
		synchronized (this.targets) {
			targets = new ArrayList<>(this.targets);
		}
		targets.add(scheduleType);
		return targets;
	}
	
	void addSource(OgemaDataSource<?, ?> source) {
		sources.add(source);
	}
	
	void removeSource(OgemaDataSource<?, ?> source) {
		sources.remove(source);
	}
	
	void addTarget(OgemaTimeSeriesPersistence<?, ?> target) {
		targets.add(target);
	}
	
	void removeTarget(OgemaTimeSeriesPersistence<?, ?> target) {
		targets.remove(target);
	}
	
	Set<OgemaDataSource<?, ?>> drainSources() {
		synchronized (sources) {
			Set<OgemaDataSource<?, ?>> result = new HashSet<>(sources);
			sources.clear();
			return result;
		}
		
	}
	
	Set<OgemaTimeSeriesPersistence<?, ?>> drainTargets() {
		synchronized (targets) {
			Set<OgemaTimeSeriesPersistence<?, ?>> result = new HashSet<>(targets);
			targets.clear();
			return result;
		}
	}
	
}
