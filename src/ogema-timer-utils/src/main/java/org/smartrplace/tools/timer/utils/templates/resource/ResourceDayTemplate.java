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

import java.time.LocalTime;
import java.util.NavigableMap;

import org.smartrplace.tools.timer.utils.model.DayTemplateData;
import org.smartrplace.tools.timer.utils.templates.DayTemplate;

class ResourceDayTemplate<T> implements DayTemplate<T> {
	
	private final DayTemplateData data;
	
	ResourceDayTemplate(DayTemplateData data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	@Override
	public NavigableMap<LocalTime, T> getDayValues() {
		return (NavigableMap<LocalTime, T>) data.getValuesAsMap();
	}
	
}
