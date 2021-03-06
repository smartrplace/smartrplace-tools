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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.smartrplace.tools.exec.ExecutorConstants;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.smartrplace.tools.upload.client.FileUploadClient;
import org.smartrplace.tools.upload.server.FileUploadConstants;

/**
 * Test base class for both client and server.
 * 
 * @author cnoelle
 * 
 */
class TestBase {

	private static final String slf4jVersion = "1.7.25";
	static final String uploadVersion = "0.0.1";
	static final int HTTP_PORT = 4712;
	protected static final Path configFile = Paths.get("data/test.config");
	protected static final Path osgiStorage = Paths.get("data/osgi-storage");
	static final Path uploadFolder = Paths.get(FileUploadConstants.DEFAULT_UPLOAD_FOLDER).resolve(TestContextHelper.TEST_USER);
	private static final AtomicInteger cnt = new AtomicInteger(0);
	
	protected TestBase() {
		this(0, 0);
	}
	
	protected TestBase(final long period, final long delay) {
		try (final InputStream in = new ByteArrayInputStream(getConfigProperty(period, delay).getBytes(StandardCharsets.UTF_8))) {
			Files.createDirectories(configFile.getParent());
			Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e)
		{
			throw new AssertionError("Could not create test config file",e);
		}
	}
	
	@Inject
	protected BundleContext ctx;
	
	@Inject
	protected FileUploadClient client;
	
	// for debugging
//	@Inject
//	private ConfigurationAdmin ca;

	@Configuration
	public Option[] configuration() throws IOException {
		return new Option[] {
				CoreOptions.cleanCaches(),
				CoreOptions.frameworkProperty("org.osgi.service.http.port").value(Integer.toString(HTTP_PORT)),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_STORAGE).value(osgiStorage.toString()),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_STORAGE_CLEAN).value(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_BSNVERSION).value(Constants.FRAMEWORK_BSNVERSION_MULTIPLE),
				CoreOptions.vmOption("-ea"), 
				CoreOptions.when(getJavaVersion() == 9).useOptions(
						CoreOptions.vmOption("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"),
						CoreOptions.vmOption("--add-modules=java.xml.bind,java.xml.ws.annotation")
					),
                CoreOptions.when(getJavaVersion() > 9).useOptions(
						CoreOptions.vmOption("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"),
						CoreOptions.mavenBundle("com.sun.activation", "javax.activation", "1.2.0"),
                        CoreOptions.mavenBundle("javax.annotation", "javax.annotation-api", "1.3.2"),
                        CoreOptions.mavenBundle("javax.xml.bind", "jaxb-api", "2.4.0-b180830.0359"),
                        CoreOptions.mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm", "2.7.3"),
                        CoreOptions.mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core", "2.7.3"),
                        CoreOptions.mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.moxy", "2.7.3")
					),
				CoreOptions.junitBundles(),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.1.6"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.9.4"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configurator", "1.0.4"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.5.0"),
//				CoreOptions.mavenBundle("org.osgi", "org.osgi.service.useradmin", "1.1.0"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin", "1.0.3"),
//				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin.filestore", "1.0.2"),
				
//				CoreOptions.mavenBundle("javax.servlet", "javax.servlet-api", "3.1.0"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.2"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "4.0.4").start(),
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-core", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-annotations", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-databind", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.module", "jackson-module-jaxb-annotations", "2.9.6"),
				CoreOptions.mavenBundle("commons-io", "commons-io", "2.6"),
				//
				CoreOptions.mavenBundle("org.slf4j", "slf4j-api", slf4jVersion),
				CoreOptions.mavenBundle("org.slf4j", "osgi-over-slf4j", slf4jVersion),
				CoreOptions.mavenBundle("org.slf4j", "slf4j-simple", slf4jVersion).noStart(),
				
				CoreOptions.mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.4.5"),
				CoreOptions.mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.5.2"),
				CoreOptions.mavenBundle("org.apache.httpcomponents", "httpasyncclient-osgi", "4.1.4"),
				
				CoreOptions.mavenBundle("org.smartrplace.tools", "executor-service", "0.0.1").start(),
				CoreOptions.mavenBundle("org.smartrplace.tools", "ssl-service", "0.0.1"),
				
				CoreOptions.mavenBundle("org.smartrplace.tools", "file-upload-utils", uploadVersion),
				CoreOptions.mavenBundle("org.smartrplace.tools", "file-upload-servlet-v2", uploadVersion).start(),
				CoreOptions.mavenBundle("org.smartrplace.tools", "file-upload-client-v2", uploadVersion).start(),
				
				CoreOptions.mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "3.0.0"),
				CoreOptions.mavenBundle("biz.aQute.bnd", "biz.aQute.bndlib", "3.5.0"),
				
				// specifying the JSON directly does not work in tests... pax exam seems to tweak the system and framework properties, removing "\""; hence we use a temp file
				CoreOptions.frameworkProperty("configurator.initial").value("file:" + configFile.toString()) };
	}
	
	private static int getJavaVersion() {
		String version = System.getProperty("java.specification.version");
		final int idx = version.indexOf('.');
		if (idx > 0)
			version = version.substring(idx + 1);
		return Integer.parseInt(version); 
	}

	protected String getConfigProperty(final long period, final long delay) {
		final Object props = convertToJson(configPropertyMap(period, delay));
		return props.toString().replace('=', ':').replace("{", "\n{\n").replace("}", "\n}\n");
	}
	
	private static final Object convertToJson(final Object obj) {
		if (obj instanceof String)
			return "\"" + obj  + "\"";
		if (!(obj instanceof Map))
			return obj; 
		final Map<String,Object> map = (Map<String, Object>) obj;
		return map.entrySet().stream()
			.collect(Collectors.toMap(entry -> "\n\"" + entry.getKey() + "\"", entry -> convertToJson(entry.getValue())));
	}
	
	protected Bundle checkBundleStarted(final String symbolidName) {
		final Bundle bundle = Arrays.stream(ctx.getBundles())
			.filter(b -> symbolidName.equals(b.getSymbolicName()))
			.findAny().orElseThrow(() -> new AssertionError("Bundle not found: " + symbolidName));
		Assert.assertEquals(symbolidName + " inactive",Bundle.ACTIVE, bundle.getState());
		return bundle;
	}
	
	protected static final String nextFilePrefix() {
		return "test" + cnt.getAndIncrement();
	}
	
	protected static Path stringToTempFile(final String string, final Path baseFolder) throws IOException {
		final InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
		final String testFilePrefix = nextFilePrefix();
		final String testFilename = testFilePrefix + ".txt";
		final Path testFile = baseFolder.resolve(testFilename);
		Files.copy(stream, testFile, StandardCopyOption.REPLACE_EXISTING);
		return testFile;
	}
	
	protected static Path stringToFile(final String string, final Path file) throws IOException {
		final InputStream stream = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
		final String testFilePrefix = nextFilePrefix();
		final String testFilename = testFilePrefix + ".txt";
		Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);
		return file;
	}
	
	// here we use the default values where possible
	protected Map<String, Object> configPropertyMap(long period, long delay) {
		final Map<String, Map<String, Object>> properties0 = Arrays
				.stream(new String[] { 
						ExecutorConstants.HOUSEKEEPING_EXEC_PID,
						FileUploadConstants.FILE_UPLOAD_PID
				})
				.collect(Collectors.toMap(Function.identity(), str -> Collections.<String, Object> emptyMap()));
		final Map<String, Object> properties = new HashMap<>(properties0);
		final Map<String, Object> clientProps = new HashMap<>(4);
		clientProps.put("remoteUrl", "http://localhost:" + HTTP_PORT + ServletConstants.DEFAULT_PATH_PREFIX + FileUploadConstants.PATH);
		clientProps.put("remoteUser", TestContextHelper.TEST_USER); // in this test setting the user is ignored
		clientProps.put("remotePw", TestContextHelper.TEST_USER);
		properties.put(FileUploadClient.CLIENT_PID, clientProps);
		final Map<String, Object> servletProps = new HashMap<>(4);
		if (delay > 0 && period > 0) {
			servletProps.put(ExecutorConstants.TASK_DELAY, delay);
			servletProps.put(ExecutorConstants.TASK_PERIOD, period);
			servletProps.put(ExecutorConstants.TASK_PROPERTIES_TIME_UNIT, ChronoUnit.MILLIS.name());
		}
		properties.put(FileUploadConstants.FILE_UPLOAD_PID, servletProps);
		properties.put(":configurator:version", "1");
		properties.put(":configurator:symbolic-name", "initConfig");
		return properties;
	}

	@Before
	public void cleanDir() throws IOException {
		Files.createDirectories(uploadFolder);
		FileUtils.cleanDirectory(uploadFolder.toFile());
	}
	
}
