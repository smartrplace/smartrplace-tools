package org.smartrplace.tools.upload.server.impl;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
		final FileConfigurations configs;
		try (final Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			 configs = jsonReader.readValue(reader);
		} catch (Exception e) {
			logger.warn("Failed to clean up config {}", configFile, e);
			return;
		}
		configs.configurations.forEach(config -> cleanUpFiles(configFile.getParent(), config));
	}
	
	private static void cleanUpFiles(final Path directory, final FileConfiguration config) {
		final NavigableMap<Instant, Path> fileMap;
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
				fileMap.put(inst, p);
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
		if (max > 0 && max < sz) {
			delete(fileMap, sz - max);
		}
		final long maxSize = config.maxSize;
		if (maxSize > 0) {
			final LinkedHashMap<Path, Long> fileSizes = new LinkedHashMap<>();
			long totalSize = 0;
			for (Path file : fileMap.values()) {
				try {
					final long size = Files.size(file);
					fileSizes.put(file, size);
					totalSize += size;
				} catch (IOException | SecurityException e) {
					logger.warn("Failed to determine size {}", file, e); 
				}
			}
			if (totalSize > maxSize) {
				fileSizes.remove(fileMap.lastEntry().getValue()); // do not remove the latest file
				deleteSizeBased(fileSizes, totalSize - maxSize);
			}
		}
	}
	
	
	private static void deleteSizeBased(final LinkedHashMap<Path, Long> fileSizes, final long toDelete) {
		long deleted = 0;
		for (Map.Entry<Path, Long> entry : fileSizes.entrySet()) {
			final Path next = entry.getKey();
			try {
				if (Files.isDirectory(next)) {
					FileUtils.deleteDirectory(next.toFile());
					logger.trace("Deleted directory {}", next);
				}
				else {
					Files.delete(next);
					logger.trace("Deleted file {}", next);
				}
			} catch (IOException | SecurityException | IllegalArgumentException e) {
				logger.warn("Failed to delete file {}", next, e);
			}
			deleted += entry.getValue();
			if (deleted >= toDelete)
				return;
		} 
	}
	
	private static void delete(final NavigableMap<Instant, Path> files, final int toDelete) {
		final Iterator<Path> valIt =files.values().iterator();
		int deletionCnt = 0;
		while (valIt.hasNext() && deletionCnt < toDelete) {
			final Path next = valIt.next();
			try {
				Files.delete(next);
				valIt.remove();
			} catch (IOException | SecurityException e) {
				logger.warn("Could not delete file {}", next, e);
			}
			deletionCnt++;
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
		try {
			Files.list(folder)
				.filter(dir -> Files.isDirectory(dir))
				.forEach(dir -> listConfigFilesRecursively(dir, config, streamBuilder));
		} catch (IOException | SecurityException e) { // we do not throw an exception, rather ignore the folder
			logger.warn("Failed to determine files in subfolders {}", folder, e);
		}
	}
	
	
}
