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
package org.smartrplace.tools.profiles.utils;

import org.smartrplace.tools.profiles.DataPoint;

public class DataPointImpl extends LabelledItemImpl implements DataPoint {
	
	private final boolean optional;
	private final DataType dataType;
	private final Class<?> type;

	public DataPointImpl(String id, Class<?> type, DataType dataType, boolean optional, String label_de, String label_en) {
		super(id, label_de, label_en);
		this.optional = optional;
		this.dataType = dataType;
		this.type = type;
	}

	@Override
	public boolean optional() {
		return optional;
	}

	@Override
	public DataType dataType() {
		return dataType; 
	}
	
	@Override
	public Class<?> typeInfo() {
		return type;
	}
	
	@Override
	public String toString() {
		return "DataPointImpl[" + id() + "]";
	}
	
	
}
