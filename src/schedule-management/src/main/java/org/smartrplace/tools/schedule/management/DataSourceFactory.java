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
