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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.InvalidSyntaxException;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.utils.DateTimeUtils;
import org.junit.Assert;

/**
 * Tests for both client and server.
 * 
 * @author cnoelle
 * 
 */
@ExamReactorStrategy(PerClass.class)
public class FileUploadTest extends SimpleContextTestBase {
	
	@Test
	@Override
	public void startupWorks() throws IOException, InvalidSyntaxException, InterruptedException {
		super.startupWorks();
	}
	
	@Test
	public void servletWorks() throws URISyntaxException, InterruptedException, ExecutionException, AssertionError, IOException {
		final String test = "testString";
		final byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
		final InputStream stream = new ByteArrayInputStream(bytes);
		final FileConfiguration cfg = new FileConfiguration();
		cfg.filePrefix = nextFilePrefix();
		cfg.fileEnding = ".json";
		final Future<HttpResponse> future;
		try {
			future = client.upload(stream, bytes.length, null, null, cfg);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		final HttpResponse response;
		try {
			response = future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new AssertionError("file upload timed out");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 200, response.getStatusLine().getStatusCode());
		final Path uploaded;
		try (final Stream<Path> stream2 = Files.list(uploadFolder)) {
			uploaded = stream2
				.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().startsWith(cfg.filePrefix))
				.findAny().orElseThrow(() -> new AssertionError("Uploaded file does not exist."));
		}
		try {
			final String content = Files.readAllLines(uploaded).stream().collect(Collectors.joining());
			Assert.assertEquals("unexpected file content in uploaded file", test, content);
		} finally {
			Files.delete(uploaded);
		}
	}
	
	@Test
	public void servletWorksWithFile() throws URISyntaxException, InterruptedException, ExecutionException, IOException {
		final String test = "testString";
		final InputStream stream = new ByteArrayInputStream(test.getBytes(StandardCharsets.UTF_8));
		final String testFilePrefix = nextFilePrefix();
		final String testFilename = testFilePrefix + ".txt";
		final Path testFile = Paths.get("data").resolve(testFilename);
		Files.copy(stream, testFile, StandardCopyOption.REPLACE_EXISTING);
		final Future<HttpResponse> future;
		try {
			future = client.upload(testFile, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		final HttpResponse response;
		try {
			response = future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new AssertionError("file upload timed out");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 200, response.getStatusLine().getStatusCode());
		final Path uploaded;
		try (final Stream<Path> stream2 = Files.list(uploadFolder)) {
			uploaded = stream2
					.filter(Files::isRegularFile)
					.filter(file -> file.getFileName().toString().startsWith(testFilePrefix))
					.findAny().orElseThrow(() -> new AssertionError("Uploaded file does not exist."));
		}
		try {
			final String content = Files.readAllLines(uploaded).stream().collect(Collectors.joining());
			Assert.assertEquals("unexpected file content in uploaded file", test, content);
		} finally {
			Files.delete(uploaded);
			Files.delete(testFile);
		}
	}
	
	private List<Path> doIncrementalUpload(final Path path, String content, final long time, final int expectedNrFiles) throws Exception {
		if (content == null)
			content = "testString";
		final InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		final ZoneId zone = ZoneId.systemDefault();
		final String now = DateTimeUtils.DEFAULT_FORMATTER_FORMAT_NO_ZONE.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zone).toLocalDateTime());
		final String testFilename = now;
		final Path testFile = path.resolve(testFilename.replace(':', '_'));
		Files.copy(stream, testFile, StandardCopyOption.REPLACE_EXISTING);
		final Future<HttpResponse> future;
		try {
			future = client.upload(path, null, null, null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		final HttpResponse response;
		try {
			response = future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new AssertionError("file upload timed out");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 200, response.getStatusLine().getStatusCode());
		final List<Path> uploaded;
		try (final Stream<Path> stream2 = Files.list(uploadFolder)) {
			uploaded = stream2
				.filter(Files::isRegularFile)
				.filter(file -> DateTimeUtils.parseAsInstant(file.getFileName().toString()) != null)
				.collect(Collectors.toList());
		}
		Assert.assertFalse("Uploaded file does not exist.", uploaded.isEmpty());
		Assert.assertEquals("More files found than expected", expectedNrFiles, uploaded.size());
		return uploaded;
	}
	
	@Test
	public void incrementalFileUploadsWork() throws Exception {
		final long now = System.currentTimeMillis();
		final Path baseFolder = Paths.get("data/test");
		Files.createDirectories(baseFolder);
		FileUtils.cleanDirectory(baseFolder.toFile());
		try {
			final String testString = "someStringForTesting";
			final Path uploaded = doIncrementalUpload(baseFolder, testString, now, 1).get(0);
			final String content = Files.readAllLines(uploaded).stream().collect(Collectors.joining());
			Assert.assertEquals("unexpected file content in uploaded file", testString, content);
			doIncrementalUpload(baseFolder, null, now + Duration.ofHours(1).toMillis(), 2);
		} finally {
			FileUtils.cleanDirectory(baseFolder.toFile());
		}
	}
	
}
