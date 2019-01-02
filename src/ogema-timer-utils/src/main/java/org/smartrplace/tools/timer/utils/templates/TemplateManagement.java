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
package org.smartrplace.tools.timer.utils.templates;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

/**
 * Edit template values of a {@link DayTemplateProvider}.
 * 
 * @param <T>
 */
public interface TemplateManagement<T> {

	DayTemplateProvider<T> getPovider();
	
	void addValue(DayOfWeek day, LocalTime time, T value);
	void addDefaultValue(LocalTime time, T value);
	
	void addValues(DayOfWeek day, Map<LocalTime, T> values);
	void addValues(Map<DayOfWeek, Map<LocalTime, T>> values);
	void setValues(DayOfWeek day, Map<LocalTime, T> values);
	void setValues(Map<DayOfWeek, Map<LocalTime, T>> values);
	
	void addDefaultValues(Map<LocalTime, T> values);
	void setDefaultValues(Map<LocalTime, T> values);
	
	void deleteValue(DayOfWeek day, LocalTime time);
	void deleteDefaultValue(LocalTime time);
	
	void clear(DayOfWeek day);
	void clearDefault();
	
}
