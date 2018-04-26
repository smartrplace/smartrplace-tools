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
