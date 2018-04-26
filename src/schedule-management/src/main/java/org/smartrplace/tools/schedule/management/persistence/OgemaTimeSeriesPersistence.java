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
package org.smartrplace.tools.schedule.management.persistence;

import java.io.IOException;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.schedule.management.imports.OgemaDataSource;

// we assume all time series persistence services can also be used to import data
public interface OgemaTimeSeriesPersistence<S extends ReadOnlyTimeSeries, I> extends TimeSeriesPersistence, OgemaDataSource<S, I> {
	
	/**
	 * Write data to target.
	 * @param data
	 * 		the time series to be stored
	 * @param highLevelOption 
	 * 		see {@link OgemaDataSource#getHighLevelOptions()}
	 * @param target
	 * 		the time series to store the data in
	 * @param replaceValue
	 * 		replace values in the range of definition of data, or simply add them?
	 * @throws IOException
	 * 		if the time series could not be stored
	 * @throws IllegalArgumentException
	 * 		if one of the arguments is not admissible
	 */
	void store(ReadOnlyTimeSeries data, S target, boolean replaceValue) throws IOException, IllegalArgumentException;

}
