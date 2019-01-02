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
package org.smartrplace.tools.timer.utils;

import java.time.Instant;
import java.time.temporal.TemporalAmount;

import org.ogema.core.application.Timer;

/**
 * A {@link Timer} that is executed periodically according to a fixed {@link TemporalAmount}.
 * 
 * @author cnoelle
 */
public interface PeriodTimer extends Timer {

	/**
	 * May return null, if the period is not set. In this case the timer should be stopped, too.
	 * @return
	 */
	TemporalAmount getPeriod();
	
	/**
	 * Null if end time is not set, or the last execution time before this timer will be destroyed.
	 * @return
	 */
	Instant getEndTime();
	
	/**
	 * Set the last execution time, after which the timer will be destroyed.
	 * @param endTime
	 */
	void setEndTime(Instant endTime);
	
	
}
