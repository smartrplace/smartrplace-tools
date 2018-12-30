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
package org.smartrplace.tools.schedule.management.imports;

import java.util.List;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.html.form.dropdown.DropdownOption;

/**
 * A time series format, such as an OGEMA schedule, log data, etc. 
 */
public interface OgemaDataSource<S extends ReadOnlyTimeSeries, I> extends DataGenerator {

	/**
	 * Intermediate selection; may return null.
	 * Examples: if data source is SlotsDb standalone, then this returns the
	 * list of known SlotsDb instances.
	 * @return
	 */
	List<I> getHighLevelOptions();
	/**
	 * @param instance
	 * 		may be null
	 * @return
	 */
	List<DropdownOption> getAllTimeseries(I instance);
	/**
	 * Here the id is the "value" of one of the dropdown options. Returns null
	 * if the id cannot be found.
	 * @param id
	 * @param instance
	 * 		 may be null, if either no high-level selection is available, or the user
	 * 		has not selected any.
	 * @return
	 */
	S getTimeseries(I instance, String id);
	void clearCache();
	
}
