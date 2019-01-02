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
package org.smartrplace.tools.schedule.management;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

class Utils {

	static boolean isValidResourceName(String name) {
		if (name == null || name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0)))
			return false;
		for (char ch : name.toCharArray()) {
			if (!Character.isJavaIdentifierPart(ch))
				return false;
		}
		return true;
	}
	
	static List<SampledValue> getValues(final ReadOnlyTimeSeries timeSeries, final long now, 
				final boolean moveStartHere, final boolean moveEndHere, final boolean moveStart0, int repeat) {
		if ((!moveStartHere && !moveEndHere && !moveStart0 && repeat == 1) || timeSeries.isEmpty())
			return timeSeries.getValues(Long.MIN_VALUE);
		long offset;
		if (moveStartHere) {
			final SampledValue start = timeSeries.getNextValue(Long.MIN_VALUE);
			offset = (start != null ? now - start.getTimestamp() : 0);
		} else if (moveEndHere) {
			final SampledValue end = timeSeries.getPreviousValue(Long.MAX_VALUE);
			offset = (end != null ? now - end.getTimestamp() : 0);
		} else { // moveStart0
			final SampledValue start = timeSeries.getNextValue(Long.MIN_VALUE);
			offset = start != null ? -start.getTimestamp() : 0;
		}
		final List<SampledValue> values = new ArrayList<>();
		for (int i=0;i<repeat;i++) {
		      appendValues(timeSeries.iterator(), offset, values);
		      if (values.isEmpty())
		    	  break;
		      final SampledValue first = values.get(0);
		      final SampledValue last = values.get(values.size()-1);
		      final long delta = (values.size() > 1 ? values.get(1).getTimestamp() - first.getTimestamp() : 1000);
		      offset = last.getTimestamp() - first.getTimestamp() + delta;
		}
		return values;
	}
	
	static List<SampledValue> getValues(final List<SampledValue> timeSeries, final long now, 
			final boolean moveStartHere, final boolean moveEndHere, final boolean moveStart0, int repeat) {
		if ((!moveStartHere && !moveEndHere && !moveStart0 && repeat == 1) || timeSeries.isEmpty())
			return timeSeries;
		long offset;
		final int sign;
		if (moveStartHere) {
			final SampledValue start = timeSeries.get(0);
			offset = now - start.getTimestamp();
			sign = 1;
		} else if (moveEndHere) {
			final SampledValue end = timeSeries.get(timeSeries.size()-1);
			offset = now - end.getTimestamp();
			sign = -1;
		} else { // moveStart0
			final SampledValue start = timeSeries.get(0);
			offset = -start.getTimestamp();
			sign = 1;
		}
		final List<SampledValue> values = new ArrayList<>();
	    final SampledValue first = timeSeries.get(0);
	    final SampledValue last = timeSeries.get(timeSeries.size()-1);
	    final long delta = (timeSeries.size() > 1 ? timeSeries.get(1).getTimestamp() - first.getTimestamp() : 1000);
	    final long size = (last.getTimestamp() - first.getTimestamp() + delta) * sign;
		for (int i=0;i<repeat;i++) {
		      appendValues(timeSeries.iterator(), offset, values);
		      if (values.isEmpty())
		    	  break;
		      offset += size;
		}
		return values;
	}
	
	private static void appendValues(Iterator<SampledValue> it, long offset,  List<SampledValue> target) {
		SampledValue sv;
		while (it.hasNext()) {
			sv = it.next();
			target.add(new SampledValue(sv.getValue(), sv.getTimestamp() + offset, sv.getQuality()));
		}
	}
	
}
