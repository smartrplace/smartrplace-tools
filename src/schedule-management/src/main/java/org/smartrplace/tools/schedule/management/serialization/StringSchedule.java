/**
 * This file is part of OGEMA.
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
package org.smartrplace.tools.schedule.management.serialization;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.serialization.jaxb.SampledString;

import static org.ogema.serialization.JaxbResource.NS_OGEMA_REST;

/**
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "string-schedule", namespace = NS_OGEMA_REST)
@XmlType(name = "StringSchedule", namespace = NS_OGEMA_REST)
public class StringSchedule extends JaxbSchedule<String> {

	public StringSchedule() {
		throw new UnsupportedOperationException("Useless constructor, just to make JAXB happy.");
	}

	public StringSchedule(ReadOnlyTimeSeries res) {
		super(res);
	}

	public StringSchedule(ReadOnlyTimeSeries res, long start, long end) {
		super(res, start, end);
	}

	@Override
	SampledString createValue(long time, Quality quality, Value value) {
		SampledString sf = new SampledString();
		sf.setTime(time);
		sf.setQuality(quality);
		sf.setValue(value);
		return sf;
	}

}
