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
package org.smartrplace.tools.schedule.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.smartrplace.tools.schedule.management.persistence.FileBasedPersistence;
import org.smartrplace.tools.schedule.management.types.CsvGenerator;
import org.smartrplace.tools.schedule.management.types.RestImporter;
import org.smartrplace.tools.schedule.management.imports.FileBasedDataGenerator;

class FileBasedDataGeneratorFactory {

	private final RestImporter restImporter = new RestImporter();
	private final CsvGenerator csvGenerator = new CsvGenerator();
	private final Set<FileBasedDataGenerator> sources = Collections.synchronizedSet(new HashSet<>());
	private final Set<FileBasedPersistence> targets = Collections.synchronizedSet(new HashSet<>());
		
	List<FileBasedDataGenerator> getSources() {
		final List<FileBasedDataGenerator> sources;
		synchronized (this.sources) {
			sources = new ArrayList<>(this.sources);
		}
		sources.add(restImporter);
		sources.add(csvGenerator);
		return sources;
	}
	
	List<FileBasedPersistence> getTargets() {
		final List<FileBasedPersistence> targets;
		synchronized (this.targets) {
			targets = new ArrayList<>(this.targets);
		}
		targets.add(restImporter);
		targets.add(csvGenerator);
		return targets;
	}
	
	void addSource(FileBasedDataGenerator source) {
		sources.add(source);
	}
	
	void removeSource(FileBasedDataGenerator source) {
		sources.remove(source);
	}

	void addTarget(FileBasedPersistence target) {
		targets.add(target);
	}
	
	void removeTarget(FileBasedPersistence target) {
		targets.remove(target);
	}
	
	Set<FileBasedDataGenerator> drainSources() {
		synchronized (sources) {
			Set<FileBasedDataGenerator> result = new HashSet<>(sources);
			sources.clear();
			return result;
		}
	}
	
	Set<FileBasedPersistence> drainTargets() {	
		synchronized (targets) {
			Set<FileBasedPersistence> result = new HashSet<>(targets);
			targets.clear();
			return result;
		}
	}
}
