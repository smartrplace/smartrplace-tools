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
package org.smartrplace.tools.schedule.management.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.FileItem;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.schedule.management.persistence.FileBasedPersistence;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class CsvGenerator implements FileBasedPersistence {
	
	@Override
	public String id() {
		return "csvgenerator";
	}

	@Override
	public String label(OgemaLocale locale) {
		return "CSV file";
	}

	@Override
	public String description(OgemaLocale locale) {
		return "Load data from a CSV file and copy it to the active schedule";
	}
	
	@Override
	public String supportedFileFormat() {
		return "csv";
	}
	
	@Override
	public String defaultOptionString() {
		return ";";
	}
	
	@Override
	public String optionDescription(OgemaLocale locale) {
		return "CSV delimiter";
	}

	@Override
	public String checkOptionsString(String options, OgemaLocale locale) {
		if (options == null || options.length() != 1) 
			return "Delimiter must be exactly one character";
		return null;
	}
	
	@Override
	public List<SampledValue> parseFile(FileItem file, Class<?> type, String separator) throws IOException {
		return parseFile(file, type, separator, Long.MIN_VALUE, Long.MAX_VALUE);
	}
	
	@Override
	public List<SampledValue> parseFile(FileItem file, Class<?> type, String separator, long start, long end) throws IOException {
		char delimiter = ';';
		if (separator != null && !separator.isEmpty()) 
			delimiter = separator.charAt(0);
		CSVFormat format = CSVFormat.DEFAULT.withDelimiter(delimiter);
		final List<SampledValue> values = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
			try (CSVParser parser = new CSVParser(reader, format)) {
				long timestamp;
				float value;
				for (CSVRecord record : parser) {
					try {
						timestamp = Long.parseLong(record.get(0));
						if (timestamp < start || timestamp > end)
							continue;
					} catch (NumberFormatException e) {
						continue;
					}
					value = Float.parseFloat(record.get(1));
					values.add(new SampledValue(new FloatValue(value), timestamp, Quality.GOOD));	
				}
			}
		}
		return values;
	}

	@Override
	public void generate(ReadOnlyTimeSeries timeSeries, String options, Class<?> type, Writer writer) throws IOException, IllegalArgumentException {
		Iterator<SampledValue> it  =timeSeries.iterator();
		SampledValue sv;
		while (it.hasNext()) {
			sv = it.next();
			writer.write(sv.getTimestamp() + options + TypeUtils.getValue(sv.getValue(), type) + "\n");
		}
		writer.flush();
	}
	
	@Override
	public String getFileEnding(String options) {
		return "csv";
	}
	
}
