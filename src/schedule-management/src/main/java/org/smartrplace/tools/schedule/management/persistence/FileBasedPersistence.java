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
import java.io.Writer;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.schedule.management.imports.FileBasedDataGenerator;

// we assume all time series persistence services can also be used to import data
public interface FileBasedPersistence extends TimeSeriesPersistence, FileBasedDataGenerator {
	
	/**
	 * @param timeSeries
	 * @throws IOException
	 * 		if the time series could not be stored
	 * @throws IllegalArgumentException
	 * 		if one of the arguments is not admissible
	 */
	void generate(ReadOnlyTimeSeries timeSeries, String options, Class<?> type, Writer writer) throws IOException, IllegalArgumentException;

	String getFileEnding(String options);
	
}
