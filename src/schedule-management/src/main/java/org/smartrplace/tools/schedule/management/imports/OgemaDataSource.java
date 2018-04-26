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
