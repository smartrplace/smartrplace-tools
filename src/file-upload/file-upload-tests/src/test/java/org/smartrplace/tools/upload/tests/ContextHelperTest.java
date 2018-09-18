package org.smartrplace.tools.upload.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.ogema.accesscontrol.PermissionManager;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.smartrplace.tools.upload.api.FileConfiguration;

/**
 * Tests for both client and server.
 * 
 * @author cnoelle
 */
@ExamReactorStrategy(PerClass.class)
public class ContextHelperTest extends TestBase {

	private static final String OGEMA_VERSION = "2.1.4-SNAPSHOT";
	
	@Inject
	private PermissionManager permMan;
	
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
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.framework.security", "2.6.1"), // FIXME start only when using Felix fwk bundle
				CoreOptions.mavenBundle("org.ogema.ref-impl", "permission-admin").version(OGEMA_VERSION).startLevel(1),
				CoreOptions.mavenBundle("org.eclipse.jetty", "jetty-servlets", "9.4.11.v20180605"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin.filestore", "1.0.2"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin", "1.0.3"),
				CoreOptions.mavenBundle("org.osgi", "org.osgi.service.useradmin", "1.1.0"),
				
				CoreOptions.mavenBundle("org.apache.commons", "commons-math3", "3.6.1"),
				CoreOptions.mavenBundle("commons-codec", "commons-codec", "1.11"),
				CoreOptions.mavenBundle("org.apache.commons", "commons-lang3", "3.7"),
				CoreOptions.mavenBundle("org.json", "json", "20170516"),
				CoreOptions.mavenBundle("com.google.guava", "guava", "23.0"),
				
				CoreOptions.mavenBundle("org.ogema.core", "api", OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.core", "models").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.core", "api").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.tools", "memory-timeseries").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "administration").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "ogema-exam-base2").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "internal-api").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "non-secure-apploader").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "app-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "resource-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "resource-access-advanced").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "security").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "persistence").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "channel-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "hardware-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "recordeddata-slotsdb").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "util").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "rest").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.tools", "resource-utils").version(OGEMA_VERSION),
				
				CoreOptions.mavenBundle("org.smartrplace.tools", "smartrplace-servlet-context", "0.0.1-SNAPSHOT").start()
		};
	}
	
	private Bundle createUser(String name, long timeout) throws InterruptedException {
		Assert.assertTrue("User creation failed",permMan.getAccessManager().createUser(name, name, false));
		Bundle b = ctx.getBundle("urp:" + name);
		long start = System.currentTimeMillis();
		while (b == null && System.currentTimeMillis() - start <= timeout) {
			Thread.sleep(100);
			b = ctx.getBundle("urp:" + name);
		}
		Assert.assertNotNull("User bundle not found",b);
		return b;
	}
	
	private void removeUser(String name) {
		permMan.getAccessManager().removeUser(name);
	}
	
	@Test
	public void startupWorks() throws IOException, InvalidSyntaxException {
		checkBundleStarted("org.ogema.ref-impl.security");
		checkBundleStarted("org.smartrplace.tools.file-upload-servlet-v2");
		final Collection<ServiceReference<ServletContextHelper>> contexts = ctx.getServiceReferences(ServletContextHelper.class,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + ServletConstants.CONTEXT_NAME + ")");
		Assert.assertNotNull("Servlet context helper missing", contexts);
		Assert.assertFalse("Servlet context helper missing", contexts.isEmpty());
		final ServiceReference<ServletContextHelper> ref = contexts.iterator().next();
		final ServletContextHelper helper = ctx.getService(ref);
		try {
			Assert.assertNotNull(helper);
		} finally {
			ctx.ungetService(ref);
		}
	}
	
	
	@Test
	public void unauthorizedUserUploadFails() throws Exception {
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
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 
				HttpServletResponse.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void authorizedUserUploadWorks() throws Exception {
		createUser(TestContextHelper.TEST_USER, 5000);
		try {
			final String test = "testString";
			final byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
			final InputStream stream = new ByteArrayInputStream(bytes);
			final FileConfiguration cfg = new FileConfiguration();
			cfg.filePrefix = nextFilePrefix();
			cfg.fileEnding = "json";
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
			Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 
					HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
		} finally {
			removeUser(TestContextHelper.TEST_USER);
		} 
	}
	

}
