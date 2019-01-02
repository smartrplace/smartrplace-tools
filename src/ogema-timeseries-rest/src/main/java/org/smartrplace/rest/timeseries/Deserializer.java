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
/**
 * Copyright 2018 Smartrplace UG
 *
 * FendoDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FendoDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.rest.timeseries;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.core.timeseries.TimeSeries;
import org.ogema.recordeddata.DataRecorderException;
import org.ogema.recordeddata.RecordedDataStorage;

abstract class Deserializer {
	
	// assuming that this character is not allowed in time series ids --> TODO ?
	final static char DELIMITER = '!'; 
	static final long MAX_SIZE = 1024 * 1024; // -> ~ 25000 SampledValues with json
	private final Reader reader;
	final HttpServletResponse resp;
	final ReadOnlyTimeSeries  timeSeries;
	final char[] partial = new char[1024];
	final char[] arr = new char[1024];
	final List<SampledValue> values = new ArrayList<>();
	Long latest;
	
	Deserializer(Reader reader, ReadOnlyTimeSeries timeSeries, HttpServletResponse resp) {
		this.reader = reader;
		this.resp = resp;
		this.timeSeries  =timeSeries;
		partial[0] = DELIMITER;
		final SampledValue latest = timeSeries.getPreviousValue(Long.MAX_VALUE);
		this.latest = latest != null ? latest.getTimestamp() : null;
	}

	boolean deserializeValues() throws IOException {
		int read = 0;
		int cnt = 0;
		while ((read = reader.read(arr, 0, arr.length)) != -1) {
			cnt += read;
			if (cnt > MAX_SIZE) {
				resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
				return false;
			}
			if (!parseBuffer(arr, 0, read)) {
				return false;
			}
			if (!values.isEmpty()) {
				Collections.sort(values);
				final SampledValue first = values.get(0);
				final SampledValue last = values.get(values.size()-1);
				if (latest != null && first.getTimestamp() <= latest) {
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Time ordering invalid");
					return false;
				}
				if (timeSeries instanceof RecordedDataStorage) {
					try {
						((RecordedDataStorage) timeSeries).insertValues(values);
					} catch (DataRecorderException e) {
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to write data to time series");
						return false;
					}
				} else if (timeSeries instanceof TimeSeries) {
					((TimeSeries) timeSeries).addValues(values);
				}
				values.clear();
				latest = last.getTimestamp();
			}
		}
		return true;
	}

	static final SampledValue applyJson(final Map<String, String> map, final HttpServletResponse resp) throws IOException {
		if (map == null)
			return null;
		if (!map.containsKey("value") || !map.containsKey("time")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Value field missing");
			return null;
		}
		String value = map.get("value");
		Long t = null;
		t = Utils.parseTimeString(map.get("time"), null);
		if (t == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp " + map.get("time"));
			return null;
		}
		final Quality q = map.containsKey("quality") && map.get("quality").equalsIgnoreCase("bad") ? Quality.BAD : Quality.GOOD;
		return new SampledValue(new FloatValue(Float.parseFloat(value)), t, q);
	}
	
	abstract boolean parseBuffer(final char[] buffer, final int start, final int end) throws IOException;
	
}
