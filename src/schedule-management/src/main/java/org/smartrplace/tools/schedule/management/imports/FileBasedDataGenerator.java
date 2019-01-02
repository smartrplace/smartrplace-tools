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
package org.smartrplace.tools.schedule.management.imports;

import java.io.IOException;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.ogema.core.channelmanager.measurements.SampledValue;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/**
 * @param <O>
 * 		Options parameter
 */
public interface FileBasedDataGenerator extends DataGenerator {
	
	/**
	 * Return e.g. "csv", "xml", etc.
	 * @return
	 */
	String supportedFileFormat();
	
	/**
	 * 
	 * @return
	 */
	String optionDescription(OgemaLocale locale);
	
	/**
	 * Return null, if String is valid, an explanation otherwise
	 * @return
	 */
	String checkOptionsString(String options, OgemaLocale locale);
	
	/**
	 * 
	 * @return
	 */
	String defaultOptionString();
	
	/**
	 * The collection should be ordered, so e.g. a List or NavigableSet.
	 */
	List<SampledValue> parseFile(FileItem file, Class<?> type, String options) throws IOException, NumberFormatException;
	
	/**
	 * The collection should be ordered, so e.g. a List or NavigableSet.
	 */
	List<SampledValue> parseFile(FileItem file, Class<?> type, String options, long start, long end) throws IOException, NumberFormatException;
	

}
