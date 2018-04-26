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
