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

import java.time.LocalTime;
import java.util.Collections;
import java.util.NavigableMap;

public interface DayTemplate<T> {

	@SuppressWarnings({ "rawtypes" })
	public static final DayTemplate EMPTY_TEMPLATE = new DayTemplate() {

		@Override
		public NavigableMap getDayValues() {
			return Collections.emptyNavigableMap();
		}
	};

	NavigableMap<LocalTime, T> getDayValues();
	
}
