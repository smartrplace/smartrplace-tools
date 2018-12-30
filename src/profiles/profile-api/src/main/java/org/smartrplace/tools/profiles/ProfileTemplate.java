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
package org.smartrplace.tools.profiles;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.iwes.widgets.template.LabelledItem;

public interface ProfileTemplate extends LabelledItem {
	
	public static final String ID_PROPERTY = "template_id";
	public static final String LABEL_PROPERTY = "label";

	List<State> states();
	List<DataPoint> primaryData();
	List<DataPoint> contextData();
	
	default List<DataPoint> derivedData() {
		return Collections.emptyList();
	}
	
	default Map<DataPoint, Object> derivedData(Map<DataPoint, Object> primaryData, Map<DataPoint, Object> contextData) {
		return Collections.emptyMap();
	}
	
}
