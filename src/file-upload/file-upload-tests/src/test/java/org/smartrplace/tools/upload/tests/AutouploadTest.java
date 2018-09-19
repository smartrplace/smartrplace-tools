package org.smartrplace.tools.upload.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.tools.autoupload.AutouploadConfiguration;
import org.smartrplace.tools.exec.ExecutorConstants;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.smartrplace.tools.upload.server.FileUploadConstants;

@ExamReactorStrategy(PerClass.class)
public class AutouploadTest extends SimpleContextTestBase {

	@Configuration
	public Option[] configuration() throws IOException {
		final Option[] superConf = super.configuration();
		final Option[] newArray = new Option[superConf.length+1];
		System.arraycopy(superConf, 0, newArray, 0, superConf.length);
		newArray[superConf.length] = CoreOptions.composite(autouploadBundle());
		return newArray;
	}
	
	public Option[] autouploadBundle() {
		return new Option[] {
			CoreOptions.mavenBundle("org.smartrplace.tools", "file-upload-autoupload", uploadVersion)
		};
	}
	
	@Inject
	private ConfigurationAdmin configAdmin;
	
	@Test
	@Override
	public void startupWorks() throws IOException, InvalidSyntaxException, InterruptedException {
		super.startupWorks();
		checkBundleStarted("org.smartrplace.tools.file-upload-autoupload");
	}
	
	@Test
	public void autouploadFileWorks() throws IOException, InterruptedException {
		final String str= "autoUploadTest" + new Random().nextInt();
		final String remotePath = "autoTestRemote";
		if (Files.isDirectory(uploadFolder.resolve(remotePath)))
			FileUtils.deleteDirectory(uploadFolder.resolve(remotePath).toFile());
		final Path temp = stringToTempFile(str, Paths.get("data"));
		org.osgi.service.cm.Configuration config = null;
		try {
			final String fn = temp.getFileName().toString();
			final int ixd = fn.indexOf('.');
			final String testFilePrefix = fn.substring(0, ixd);
			config = configAdmin.getFactoryConfiguration(AutouploadConfiguration.FACTORY_PID, "test", "?");
			final Dictionary<String, Object> dict = new Hashtable<>();
			dict.put("remoteUrl", "http://localhost:" + HTTP_PORT + ServletConstants.DEFAULT_PATH_PREFIX + FileUploadConstants.PATH);
			dict.put("localPath", temp.toString());
			dict.put("remotePath", remotePath);
			dict.put(ExecutorConstants.TASK_PERIOD, 1000L);
			dict.put(ExecutorConstants.TASK_DELAY, 1L);
			dict.put(ExecutorConstants.TASK_PROPERTIES_TIME_UNIT, TimeUnit.SECONDS.name());
			config.update(dict);
			final long start = System.currentTimeMillis();
			Path uploaded = null;
			while ((uploaded == null || !Files.exists(uploaded)) && System.currentTimeMillis() - start <= 20000) {
				Thread.sleep(200);
				if (!Files.isDirectory(uploadFolder.resolve(remotePath)))
					continue;
				try (final Stream<Path> stream = Files.list(uploadFolder.resolve(remotePath))) {
					uploaded = stream
						.filter(Files::isRegularFile)
						.filter(file -> file.getFileName().toString().startsWith(testFilePrefix))
						.findAny().orElse(null);
				}
			}
			Assert.assertNotNull("Uploaded file not found",uploaded);
			Assert.assertTrue("Uploaded file is broken",Files.isRegularFile(uploaded));
			Assert.assertEquals("Unexpected content in uploaded file", str, new String(Files.readAllBytes(uploaded), StandardCharsets.UTF_8));
		} finally {
			Files.delete(temp);
			if (config != null)
				config.delete();
		}
	}
	
	@Test
	public void autouploadFolderIncrementalWorks() throws IOException, InterruptedException {
		final String str= "autoUploadTest" + new Random().nextInt();
		final String remotePath = "autoTestRemoteInc";
		final Path testFolderLocal = Paths.get("data/testlocal");
		if (Files.exists(testFolderLocal)) {
			FileUtils.deleteDirectory(testFolderLocal.toFile());
		}
		Files.createDirectories(testFolderLocal);
		final String prefix = "testfile";
		final Path file1 = testFolderLocal.resolve(prefix + System.currentTimeMillis() + ".txt");
		stringToFile(str, file1);
		org.osgi.service.cm.Configuration config = null;
		try {
			config = configAdmin.getFactoryConfiguration(AutouploadConfiguration.FACTORY_PID, "test", "?");
			final Dictionary<String, Object> dict = new Hashtable<>();
			dict.put("remoteUrl", "http://localhost:" + HTTP_PORT + ServletConstants.DEFAULT_PATH_PREFIX + FileUploadConstants.PATH);
			dict.put("localPath", testFolderLocal.toString().replace('\\', '/'));
			dict.put("remotePath", remotePath);
			dict.put("incrementalUploadFilePrefix", prefix);
			dict.put("incrementalUpload", true);
			dict.put(ExecutorConstants.TASK_PERIOD, 1L); 
			dict.put(ExecutorConstants.TASK_DELAY, 1L);
			dict.put(ExecutorConstants.TASK_PROPERTIES_TIME_UNIT, TimeUnit.SECONDS.name());
			config.update(dict);
			long start = System.currentTimeMillis();
			Path uploaded1 = null;
			while ((uploaded1 == null || !Files.exists(uploaded1)) && System.currentTimeMillis() - start <= 20000) {
				Thread.sleep(200);
				if (!Files.isDirectory(uploadFolder.resolve(remotePath)))
					continue;
				try (final Stream<Path> stream = Files.list(uploadFolder.resolve(remotePath))) {
					uploaded1 = stream
						.filter(Files::isRegularFile)
						.filter(file -> file.getFileName().toString().startsWith(prefix))
						.findAny().orElse(null);
				}
			}
			Assert.assertNotNull("Uploaded file not found",uploaded1);
			Assert.assertTrue("Uploaded file is broken",Files.isRegularFile(uploaded1));
			Assert.assertEquals("Unexpected content in uploaded file", str, new String(Files.readAllBytes(uploaded1), StandardCharsets.UTF_8));
			final String str2= "autoUploadTest" + new Random().nextInt();
			final Path file2 = testFolderLocal.resolve(prefix + System.currentTimeMillis() + ".txt");
			stringToFile(str2, file2);
			final Path firstFile = uploaded1;
			Path uploaded2 = null;
			while ((uploaded2 == null || !Files.exists(uploaded2)) && System.currentTimeMillis() - start <= 20000) {
				Thread.sleep(200);
				if (!Files.isDirectory(uploadFolder.resolve(remotePath)))
					continue;
				try (final Stream<Path> stream = Files.list(uploadFolder.resolve(remotePath))) {
					uploaded2 = stream
						.filter(Files::isRegularFile)
						.filter(file -> !file.equals(firstFile))
						.filter(file -> file.getFileName().toString().startsWith(prefix))
						.findAny().orElse(null);
				}
			}
			Assert.assertNotNull("Uploaded file not found",uploaded2);
			Assert.assertTrue("Uploaded file is broken",Files.isRegularFile(uploaded2));
			Assert.assertEquals("Unexpected content in uploaded file", str2, new String(Files.readAllBytes(uploaded2), StandardCharsets.UTF_8));
		} finally {
			FileUtils.deleteDirectory(testFolderLocal.toFile());
			if (config != null)
				config.delete();
		}
	}
	
}
