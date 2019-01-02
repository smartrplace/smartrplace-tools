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

/**
 * 
 * @author jlapp
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ScheduleEntry<T> {

	long time;
	T value;

	public ScheduleEntry() {
	}

	public ScheduleEntry(long time, T value) {
		this.time = time;
		this.value = value;
	}

}
