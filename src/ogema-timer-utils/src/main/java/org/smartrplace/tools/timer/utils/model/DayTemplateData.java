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
package org.smartrplace.tools.timer.utils.model;

import java.time.LocalTime;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.tools.resource.util.ValueResourceUtils;

public interface DayTemplateData extends Resource {

	ResourceList<TemplateData> data();
	
	default NavigableMap<LocalTime, Object> getValuesAsMap() {
		final NavigableMap<LocalTime, Object> map = new TreeMap<>();
		data().getAllElements().forEach(data -> {
			final LocalTime time = data.getLocalTime();
			if (time == null || !data.value().isActive())
				return;
			final Object o = ValueResourceUtils.getValue(data.value());
			if (o != null)
				map.put(time, o);
		});
		return map;
	}
	
}
