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
 * FendoDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FendoDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.rest.timeseries;

class Parameters {
	
	// Params to identify the data source // single value
	static final String PARAM_PROVIDER = "provider";
	// values must be of the form "key:value"
	static final String PARAM_SELECTION_OPTION = "option"; 
	
	
    static final String PARAM_DB = "db"; // string
    final static String PARAM_FORMAT = "format"; // can be used to override Accept-Header
    final static String PARAM_ID = "id"; // timeseries id
    final static String PARAM_TIMESTAMP = "time";
    final static String PARAM_TARGET = "target"; // target operation
    final static String PARAM_START = "start"; // date(-time) or long
    final static String PARAM_END = "end"; // date(-time) or long
    final static String PARAM_INTERVAL = "interval"; // long; interval in ms
    final static String PARAM_MAX = "max"; // integer; max nr 
    final static String PARAM_INDENT = "indent"; // integer; set to negative value to disable pretty-printing
    final static String PARAM_DT_FORMATTER = "datetimeformat"; // format pattern
    final static String PARAM_UPDATE_MODE = "updatemode"; // see StorageType constants
    // for searching
    final static String PARAM_PROPERTIES = "properties"; // multiple values allowed; each value must be of the form "key=value"
    final static String PARAM_TAGS = "tags"; // multiple values allowed
    final static String PARAM_ID_EXCLUDED = "idexcluded"; // timeseries id
    // statistics FIXME conlicts with PARAM_PROVIDER
    final static String PARAM_PROVIDERS = "provider";

    final static String TARGET_TIMESERIES = "timeseries";
    final static String TARGET_DB = "database";
    final static String TARGET_DATA = "data";
    final static String TARGET_VALUE = "value";
    final static String TARGET_VALUES = "values";
    final static String TARGET_PROPERTIES = PARAM_PROPERTIES;
    final static String TARGET_TAGS = PARAM_TAGS;
    final static String TARGET_TAG = "tag";
    final static String TARGET_FIND = "find";
    final static String TARGET_STATISTICS = "stats";
    final static String TARGET_SIZE = "size";
    final static String TARGET_NEXT = "nextvalue";
    final static String TARGET_PREVIOUS = "previousvalue";

}
