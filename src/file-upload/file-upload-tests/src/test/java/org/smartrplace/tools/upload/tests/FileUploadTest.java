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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.smartrplace.tools.servlet.api.ServletAccessControl;
import org.smartrplace.tools.servlet.api.ServletConstants;
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
public class FileUploadTest extends TestBase {

	private ServiceRegistration<ServletContextHelper> contextReg;
	private ServiceRegistration<ServletAccessControl> contextReg2;
	
	
	@Configuration
	public Option[] configuration() throws IOException {
		final Option[] superConf = super.configuration();
		final Option[] newArray = new Option[superConf.length+1];
		System.arraycopy(superConf, 0, newArray, 0, superConf.length);
		newArray[superConf.length] = CoreOptions.composite(ogemaBundles());
		return newArray;
	}
	
	public Option[] ogemaBundles() {
		return new Option[] {
			CoreOptions.wrappedBundle(CoreOptions.maven("org.smartrplace.tools", "smartrplace-servlet-api", "0.0.1-SNAPSHOT"))
		};
	}
	
	@Before
	public void registerFilter() throws IOException, BundleException {
//		servletApi = ctx.installBundle("test:servlet-api", TinyBundles.bundle()
//			.add(ServletAccessControl.class)
//			.add(ServletConstants.class)
//			.set("Export-Package", ServletAccessControl.class.getPackage().getName())
//			.build());
//		servletApi.start();
		final Dictionary<String, Object> properties = new Hashtable<>(4);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, ServletConstants.CONTEXT_NAME);
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, ServletConstants.DEFAULT_PATH_PREFIX);
		this.contextReg = ctx.registerService(ServletContextHelper.class, new TestContextHelper(), properties);
		this.contextReg2 = ctx.registerService(ServletAccessControl.class, new TestAccessControl(), properties);
	}
	
	@After
	public void unregisterFilter() throws BundleException {
		if (contextReg != null)
			contextReg.unregister();
		if (contextReg2 != null)
			contextReg2.unregister();
	}
	
	@Test
	public void startupWorks() throws IOException, InvalidSyntaxException, InterruptedException {
//		final org.osgi.service.cm.Configuration[] configs = ca.listConfigurations(null);
//		if (configs == null)
//			System.out.println("  null");
//		else
//			Arrays.stream(configs).forEach(cfg -> System.out.println(" Config " + cfg.getPid() + ": " + cfg));
		checkBundleStarted("org.smartrplace.tools.file-upload-servlet-v2");
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
		final Path uploaded = Files.list(uploadFolder)
				.filter(Files::isRegularFile)
				.filter(file -> file.getFileName().toString().startsWith(cfg.filePrefix))
				.findAny().orElseThrow(() -> new AssertionError("Uploaded file does not exist."));
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
		final Path uploaded = Files.list(uploadFolder)
			.filter(Files::isRegularFile)
			.filter(file -> file.getFileName().toString().startsWith(testFilePrefix))
			.findAny().orElseThrow(() -> new AssertionError("Uploaded file does not exist."));
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
		final List<Path> uploaded = Files.list(uploadFolder)
				.filter(Files::isRegularFile)
				.filter(file -> DateTimeUtils.parseAsInstant(file.getFileName().toString()) != null)
				.collect(Collectors.toList());
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
