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
