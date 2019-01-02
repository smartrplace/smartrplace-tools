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
package org.smartrplace.tools.upload.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.api.FileConfigurations;
import org.smartrplace.tools.upload.server.FileUploadConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.junit.Assert;

@ExamReactorStrategy(PerClass.class)
public class CleanUpTest extends SimpleContextTestBase {
	
	private final ObjectMapper mapper = new ObjectMapper();
	private final ObjectWriter jsonMapWriter = mapper.writerFor(FileConfigurations.class);

	
	// run clean up task every two seconds
	public CleanUpTest() {
		super(2000, 2000);
	}
	
	@Before
	public void cleanUpTestFolder() throws IOException {
		if (Files.isDirectory(uploadFolder))
			FileUtils.cleanDirectory(uploadFolder.toFile());
		else
			Files.createDirectories(uploadFolder);
	}

	@Test
	public void fileCleanUpNumberBasedWorks() throws IOException, InterruptedException {
		final FileConfigurations configs = new FileConfigurations();
		final FileConfiguration config = new FileConfiguration();
		final String prefix = "tst";
		final String suffix = ".txt";
		config.filePrefix = prefix;
		config.fileEnding = suffix;
		config.maxFilesToKeep = 2;
		configs.configurations.add(config);
		final Path cfgFile = uploadFolder.resolve(FileUploadConstants.DEFAULT_CONFIG_FILE);
		final String testString = "testme";
		final int nrFiles = 5;
		IntStream.range(0, nrFiles).forEach(i -> createFile(uploadFolder.resolve(prefix + "_" + i + suffix), testString + i));
		try (final Stream<Path> files = Files.list(uploadFolder)) {
			Assert.assertTrue(
				files.filter(p -> p.getFileName().toString().startsWith(prefix) && p.getFileName().toString().endsWith(suffix))
					.count() >= 5);
		}
		writeConfig(cfgFile, configs);
		Thread.sleep(2200);
		final long now = System.currentTimeMillis();
		while (getNrFiles(prefix, suffix, uploadFolder) > config.maxFilesToKeep  && System.currentTimeMillis() - now < 5000) {
			Thread.sleep(500);
		}
		Assert.assertEquals("Unexpected number of matching files in the upload directory", config.maxFilesToKeep, getNrFiles(prefix, suffix, uploadFolder));
	}
	
	@Test
	public void fileCleanUpSizeBasedWorks() throws IOException, InterruptedException {
		final FileConfigurations configs = new FileConfigurations();
		final FileConfiguration config = new FileConfiguration();
		final String prefix = "tst";
		final String suffix = ".txt";
		final Path cfgFile = uploadFolder.resolve(FileUploadConstants.DEFAULT_CONFIG_FILE);
		final String testString = "testme";
		final int nrFiles = 5;
		IntStream.range(0, nrFiles).forEach(i -> createFile(uploadFolder.resolve(prefix + "_" + i + suffix), testString + i));
		try (final Stream<Path> files = Files.list(uploadFolder)) {
			Assert.assertTrue(
				files.filter(p -> p.getFileName().toString().startsWith(prefix) && p.getFileName().toString().endsWith(suffix))
					.count() >= 5);
		}
		final long fileSize;
		try (final Stream<Path> files = Files.list(uploadFolder)) {
				fileSize = Files.size(files.filter(p -> p.getFileName().toString().startsWith(prefix) && p.getFileName().toString().endsWith(suffix))
					.findAny().get());
		}
		config.filePrefix = prefix;
		config.fileEnding = suffix;
		final int maxFilesToKeep = 2;
		config.maxSize = maxFilesToKeep * fileSize + 1;
		configs.configurations.add(config);
		writeConfig(cfgFile, configs);
		Thread.sleep(2200);
		final long now = System.currentTimeMillis();
		while (getNrFiles(prefix, suffix, uploadFolder) > maxFilesToKeep  && System.currentTimeMillis() - now < 5000) {
			Thread.sleep(500);
		}
		Assert.assertEquals("Unexpected number of matching files in the upload directory", maxFilesToKeep, getNrFiles(prefix, suffix, uploadFolder));
	}
	
	// ensure no files are deleted if config does not demand it
	@Test
	public void fileCleanUpRespectsConfig() throws IOException, InterruptedException {
		final FileConfigurations configs = new FileConfigurations();
		final FileConfiguration config = new FileConfiguration();
		final String prefix = "tst";
		final String suffix = ".txt";
		config.filePrefix = prefix;
		config.fileEnding = suffix;
		configs.configurations.add(config);
		final Path cfgFile = uploadFolder.resolve(FileUploadConstants.DEFAULT_CONFIG_FILE);
		final String testString = "testme";
		final int nrFiles = 5;
		IntStream.range(0, nrFiles).forEach(i -> createFile(uploadFolder.resolve(prefix + "_" + i + suffix), testString + i));
		writeConfig(cfgFile, configs);
		Thread.sleep(4000);
		Assert.assertEquals("Unexpected number of matching files in the upload directory", nrFiles, getNrFiles(prefix, suffix, uploadFolder));
	}	
	
	private final long getNrFiles(final String prefix, final String suffix, final Path folder) throws IOException {
		try (final Stream<Path> stream = Files.list(folder)) {
			Stream<Path> stream2 = stream;
			if (prefix != null)
				stream2 = stream2.filter(p -> p.getFileName().toString().startsWith(prefix));
			if (suffix != null)
				stream2 = stream2.filter(p -> p.getFileName().toString().endsWith(suffix));
			return stream2.count();
		}
	}
	
	private final void createFile(final Path p, final String content) {
		final InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		try {
			Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private final void writeConfig(final Path configFile, final FileConfigurations configs) throws IOException {
		try (final Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			jsonMapWriter.writeValue(writer, configs);
		}
	}
	
}
