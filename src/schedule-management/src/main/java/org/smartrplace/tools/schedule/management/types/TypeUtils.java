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
