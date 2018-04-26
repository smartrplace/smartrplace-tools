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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import static org.ogema.serialization.JaxbResource.NS_OGEMA_REST;

import org.ogema.serialization.jaxb.SampledBoolean;

/**
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "resource", namespace = NS_OGEMA_REST)
@XmlType(name = "BooleanSchedule", namespace = NS_OGEMA_REST)
public class BooleanSchedule extends JaxbSchedule<Boolean> {

	public BooleanSchedule() {
		throw new UnsupportedOperationException("Useless constructor, just to make JAXB happy.");
	}

	public BooleanSchedule(ReadOnlyTimeSeries res) {
		super(res);
	}

	public BooleanSchedule(ReadOnlyTimeSeries res, long start, long end) {
		super(res, start, end);
	}

	@Override
	SampledBoolean createValue(long time, Quality quality, Value value) {
		SampledBoolean sf = new SampledBoolean();
		sf.setTime(time);
		sf.setQuality(quality);
		sf.setValue(value);
		return sf;
	}

}
