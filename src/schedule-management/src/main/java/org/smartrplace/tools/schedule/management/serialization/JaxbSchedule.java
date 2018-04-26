/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.tools.schedule.management.serialization;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.serialization.jaxb.SampledFloat;
import org.ogema.serialization.jaxb.SampledValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static org.ogema.serialization.JaxbResource.NS_OGEMA_REST;

/**
 * Dummy implementation of a JAXB-compatible wrapper for OGEMA schedules.
 * 
 * 
 * @author jlapp
 * @param <T>
 *            type of schedule values.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
// @XmlRootElement(name = "resource", namespace = NS_OGEMA_REST)
@XmlType(name = "ScheduleResource", namespace = NS_OGEMA_REST, propOrder = { "interpolationMode", "lastUpdateTime",
		"lastCalculationTime", "start", "end", "entry" })
// FIXME SampledInteger, etc.?
@XmlSeeAlso( { ScheduleEntry.class, BooleanSchedule.class, FloatSchedule.class, IntegerSchedule.class,
		OpaqueSchedule.class, StringSchedule.class, TimeSchedule.class, SampledValue.class, SampledFloat.class })
public abstract class JaxbSchedule<T> {

	protected long start = 0;
	protected long end = Long.MAX_VALUE;
	protected ReadOnlyTimeSeries schedule;
	

	public JaxbSchedule() {
		throw new UnsupportedOperationException("Useless constructor, just to make JAXB happy.");
	}

	public JaxbSchedule(ReadOnlyTimeSeries schedule, long start, long end) {
		this.schedule = schedule;
		this.start = start;
		this.end = end;
	}

	public JaxbSchedule(ReadOnlyTimeSeries res, long start) {
		this(res, start, Long.MAX_VALUE);
	}

	public JaxbSchedule(ReadOnlyTimeSeries res) {
		this(res, 0, Long.MAX_VALUE);
	}

	@XmlElement(required = false)
	public String getInterpolationMode() {
		return schedule.getInterpolationMode().name();
	}

	@XmlElement(required = false)
	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	@XmlElement(required = false)
	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	@XmlElements({ @XmlElement(name = "entry", type = ScheduleEntry.class) })
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
	public List<SampledValue> getEntry() {
		final ReadOnlyTimeSeries schedule = this.schedule;
		List<org.ogema.core.channelmanager.measurements.SampledValue> values;
        if (start == -1){
            values = schedule.getValues(0);
        } else {
            if (end == -1){
                values = schedule.getValues(start);
            } else {
                values = schedule.getValues(start, end);
            }
        }
		List<SampledValue> result = new ArrayList<>(values.size());
		for (org.ogema.core.channelmanager.measurements.SampledValue value : values) {
			result.add(createValue(value.getTimestamp(), value.getQuality(), value.getValue()));
		}
		return result;
	}

	abstract SampledValue createValue(long time, Quality quality, Value value);

}
