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
package org.smartrplace.tools.upload.server.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.apache.commons.io.FileUtils;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.exec.ExecutorConstants;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.api.FileConfigurations;
import org.smartrplace.tools.upload.server.FileUploadConstants;
import org.smartrplace.tools.upload.utils.DateTimeUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Component(
		service=Runnable.class,
		property= {
				ExecutorConstants.TASK_DELAY +":Long=6",
				ExecutorConstants.TASK_PERIOD + ":Long=6",
				ExecutorConstants.TASK_PROPERTIES_TIME_UNIT + "=HOURS"
		},
		// using the same PID as the upload service, to ensure we get the same props
		configurationPid=FileUploadConstants.FILE_UPLOAD_PID,
		configurationPolicy=ConfigurationPolicy.REQUIRE
)
public class FileUploadHousekeeping implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(FileUploadHousekeeping.class);
	private final ObjectReader jsonReader = new ObjectMapper().readerFor(FileConfigurations.class);
	private volatile FileUploadConfiguration config;
	
	@Activate
	protected void activate(FileUploadConfiguration config) {
		this.config = config;
		try {
			Paths.get(config.uploadFolder());
		} catch (InvalidPathException e) {
			throw new ComponentException(e);
		}
	}
	
	@Override
	public void run() {
		final FileUploadConfiguration config = this.config;
		try (final Stream<Path> stream = Files.list(Paths.get(config.uploadFolder()))) {
			stream
				.filter(Files::isDirectory)
				.flatMap(this::getConfigFilesRecursively)
				.forEach(this::cleanUp);
		} catch (IOException | SecurityException e) {
			logger.error("Failed to execute housekeeping task",e);
		}
	}

	private void cleanUp(Path configFile) {
		try {
			final FileConfigurations configs;
			try (final Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
				 configs = jsonReader.readValue(reader);
			}
			configs.configurations.forEach(config -> cleanUpFiles(configFile.getParent(), config));
		} catch (IOException | SecurityException e) {
			logger.error("Failed to execute housekeeping task for {}", configFile,e);
		}
	}
	
	private static void cleanUpFiles(final Path directory, final FileConfiguration config) {
		final Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
		final NavigableMap<Instant, FileInfo> fileMap;
		try (final Stream<Path> files = Files.list(directory)) {
			final Iterator<Path> it = files.iterator();
			fileMap = new TreeMap<>();
			while (it.hasNext()) {
				final Path p = it.next();
				if (!Files.exists(p)) 
					continue;
				if (!prefixSuffiXMatch(p, config))
					continue;
				final Instant inst = DateTimeUtils.parseFilenameTimestamp(p);
				if (inst == null) {
					logger.warn("Filename timestamp cannot be parsed {}",p);
					continue;
				}
				if (config.daysToKeepFile > 0) {
					final Duration duration = Duration.between(inst, now);
					if (duration.compareTo(Duration.ofDays(config.daysToKeepFile)) > 0) {
						try {
							if (Files.isDirectory(p)) {
								FileUtils.deleteDirectory(p.toFile());
							} else {
								Files.delete(p);
							}
							continue;
						} catch (IOException | IllegalArgumentException e) {
							logger.warn("Failed to delete file {}", p, e);
						}
					}
				}
				try {
					fileMap.put(inst, new FileInfo(p, inst.toEpochMilli()));
				} catch (IOException | SecurityException e) {}
			}
		} catch (Exception e) {
			logger.warn("Failed to clean up in directory {}", directory);
			return;
		}
		if (fileMap.isEmpty())
			return;
		final int sz = fileMap.size();
		if (sz == 1)
			return;
		final int max = config.maxFilesToKeep;
		final long maxSize = config.maxSize;
		final long totalSize = fileMap.values().stream()
				.mapToLong(info -> info.size)
				.sum();
		if ((max > 0 && max < sz) || (maxSize > 0 && totalSize > maxSize)) {
			delete(fileMap, sz - max, totalSize - maxSize, config.deleteOldFilesFirst);
		}
	}
	
	/*
	private static void deleteSizeBased(final NavigableMap<Instant, FileInfo> files, final long toDelete) {
		long deleted = 0;
		for (FileInfo info : files.values()) {
			try {
				final Path file = info.file;
				if (Files.isDirectory(file)) {
					FileUtils.deleteDirectory(file.toFile());
					logger.trace("Deleted directory {}", file);
				}
				else {
					Files.delete(file);
					logger.trace("Deleted file {}", file);
				}
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				logger.warn("Failed to delete file {}", info.file, e);
			}
			deleted += info.size;
			if (deleted >= toDelete)
				return;
		} 
	}
	*/
	
	private static void delete(final NavigableMap<Instant, FileInfo> files, 
			final int toDelete, final long sizeToDelete, final boolean deleteOldestFiles) {
		final Iterator<FileInfo> valIt =files.values().iterator();
		if (!valIt.hasNext())
			return;
		int deletionCnt = 0;
		long deletionCntSize = 0;
		if (deleteOldestFiles || toDelete >= files.size()) {
			while (valIt.hasNext() && (deletionCnt < toDelete || deletionCntSize < sizeToDelete) ) {
				final FileInfo next = valIt.next();
				try {
					Files.delete(next.file);
					valIt.remove();
				} catch (IOException | SecurityException e) {
					logger.warn("Could not delete file {}", next.file, e);
				}
				// we should increase the count independently of whether the deletion succeeded... otherwise we'd end up deleting the wrong files
				deletionCnt++;
				deletionCntSize += next.size;
			}
		} else {
			FileInfo lastFile = valIt.next();
			while (valIt.hasNext() && (deletionCnt < toDelete || deletionCntSize < sizeToDelete)) {
				final FileInfo next = valIt.next();
				if (lastFile.size == next.size) { // if the size of two consecutive files is equal, there is a high probability of the files being equal...
					try {
						Files.delete(lastFile.file);
						valIt.remove();
					} catch (IOException | SecurityException e) {
						logger.warn("Could not delete file {}", next.file, e);
					}
					deletionCnt++;
					deletionCntSize += next.size;
				}
				lastFile = next;
			}
			outer : while (deletionCnt < toDelete || deletionCntSize < sizeToDelete) {
				int missing = toDelete - deletionCnt;
				if (missing <= 0) {
					final int averageSize = (int) (files.values().stream()
							.mapToLong(info -> info.size)
							.sum() / files.size());
					missing = Math.max(1, (int) (sizeToDelete / averageSize / 2));
				}
				final long first = files.firstKey().toEpochMilli();
				final long last = files.lastKey().toEpochMilli();
				for (int i=0; i < missing; i++) {
					final long t = first + (last - first) * (long) Math.tanh(0.3 + i / 2.);
					final Map.Entry<Instant, FileInfo> entry = files.ceilingEntry(Instant.ofEpochMilli(t));
					if (entry == null) {
						if (i == 0)
							break outer;
						break;
					}
					try {
						Files.delete(entry.getValue().file);
						files.remove(entry.getKey());
					} catch (IOException | SecurityException e) {
						logger.warn("Could not delete file {}", entry.getValue().file, e);
					}
					deletionCnt++;
					deletionCntSize += entry.getValue().size;
				}
			}
		}
	}
	
	private static boolean prefixSuffiXMatch(final Path path, final FileConfiguration config) {
		if (!Files.isRegularFile(path))
			return false;
		final String filename = path.getFileName().toString();
		return filename.startsWith(config.filePrefix) && (config.fileEnding == null || filename.endsWith(config.fileEnding)); 
	}
	
	private Stream<Path> getConfigFilesRecursively(final Path base) {
		final Builder<Path> builder = Stream.builder();
		listConfigFilesRecursively(base, config, builder);
		return builder.build();
	}
	
	private static void listConfigFilesRecursively(final Path folder, 
				final FileUploadConfiguration config, final Builder<Path> streamBuilder) {
		Path thisConfig = folder.resolve(config.configFileName());
		if (Files.isRegularFile(thisConfig))
			streamBuilder.add(thisConfig);
		try (final Stream<Path> stream = Files.list(folder)) {
			stream
				.filter(dir -> Files.isDirectory(dir))
				.forEach(dir -> listConfigFilesRecursively(dir, config, streamBuilder));
		} catch (IOException | SecurityException e) { // we do not throw an exception, rather ignore the folder
			logger.warn("Failed to determine files in subfolders {}", folder, e);
		}
	}
	
	
}
