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
