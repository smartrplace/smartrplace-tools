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

import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.schedule.management.serialization.BooleanSchedule;
import org.smartrplace.tools.schedule.management.serialization.FloatSchedule;
import org.smartrplace.tools.schedule.management.serialization.IntegerSchedule;
import org.smartrplace.tools.schedule.management.serialization.JaxbSchedule;
import org.smartrplace.tools.schedule.management.serialization.OpaqueSchedule;
import org.smartrplace.tools.schedule.management.serialization.TimeSchedule;

class TypeUtils {

	static final Object getValue(Value val, Class<?> type) {
		if (type == null || type  == Float.class)
			return val.getFloatValue();
		if (type == Integer.class)
			return val.getIntegerValue();
		if (type == Long.class)
			return val.getLongValue();
		if (type == Boolean.class)
			return val.getBooleanValue();
		if (type == String.class)
			return val.getStringValue();
		if (type == Double.class)
			return val.getDoubleValue();
		if (type == Byte.class)
			return val.getByteArrayValue();
		if (type == ReadOnlyTimeSeries.class)
			return val.getTimeSeriesValue();
		return val.getObjectValue();
	}
	
	static final JaxbSchedule<?> getSchedule(ReadOnlyTimeSeries rots,Class<?> type) {
		if (type ==null || type == Float.class)
			return new FloatSchedule(rots);
		if (type == Integer.class)
			return new IntegerSchedule(rots);
		if (type == Long.class)
			return new TimeSchedule(rots);
		if (type == Boolean.class)
			return new BooleanSchedule(rots);
		if (type == Byte.class)
			return new OpaqueSchedule(rots);
		throw new IllegalArgumentException("Type " + type.getName() + " not admissible");
		
	}
	
	
}
