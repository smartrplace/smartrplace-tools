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
