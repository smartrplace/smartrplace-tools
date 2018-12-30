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
package org.smartrplace.tools.profiles;

import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.tools.profiles.DataPoint.DataType;

import de.iwes.widgets.template.LabelledItem;

public interface Profile extends LabelledItem {
	
	/**
	 * 
	 * Allowed return values are
	 * <ul>
	 *   <li>{@link ReadOnlyTimeSeries} if dp is of {@link DataPoint#dataType() type} {@link DataType#TIME_SERIES}
	 *   <li>String if dp is of {@link DataPoint#dataType() type} {@link DataType#STRING}
	 *   <li>Number if dp is of {@link DataPoint#dataType() type} {@link DataType#SINGLE_VALUE}
	 * </ul>
	 * @param dp
	 * @return
	 */
	Object getPrimaryData(DataPoint dp);
	Object getContextData(DataPoint dp);
	Object getDerivedData(DataPoint dp);
	Map<DataPoint, Object> getPrimaryData();
	Map<DataPoint, Object> getContextData();
	Map<DataPoint, Object> getDerivedData();
	Map<Long, State> stateEndTimes();
 	 
	String templateId();

}
