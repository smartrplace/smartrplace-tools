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
package org.smartrplace.tools.timer.utils.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.TimeResource;

public interface TemplateData extends Resource {

	/**
	 * To be interpreted as milliseconds since start of day
	 * @return
	 */
	TimeResource time();
	
	ValueResource value();
	
	default LocalTime getLocalTime() {
		if (!time().isActive())
			return null;
		final long t = time().getValue();
		if (t < 0 || t > Duration.ofDays(1).toMillis())
			return null;
		return LocalTime.ofNanoOfDay(t * 1000000);
	}
	
	default void setLocalTime(LocalTime time) {
		Objects.requireNonNull(time);
		time().<TimeResource> create().setValue(time.toNanoOfDay()/1000000);
	}
	
}
