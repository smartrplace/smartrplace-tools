package org.smartrplace.tools.upload.tests;

import java.io.ByteArrayInputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.smartrplace.tools.upload.api.FileConfiguration;
import org.smartrplace.tools.upload.client.FileUploadClient;
import org.smartrplace.tools.upload.server.FileUploadConstants;

/**
 * Tests for both client and server.
 * 
 * @author cnoelle
 */
// FIXME config admin and configurator versions in test base!
@ExamReactorStrategy(PerClass.class)
public class ContextSecurityTest extends SecurityTestBase {

	private static final String OGEMA_VERSION = "2.2.0";
	private final static AtomicInteger permissionCnt = new AtomicInteger(0);
	private final static AtomicInteger userCnt = new AtomicInteger(0);

	@Inject
	private ConditionalPermissionAdmin cpa;
	
	@Inject
	private PermissionManager permMan;
	
	@Inject
	private ConfigurationAdmin configAdmin;
	
	@Configuration
	public Option[] configuration() throws IOException {
		final Option[] superConf = super.configuration();
		final Option[] newArray = new Option[superConf.length+1];
		System.arraycopy(superConf, 0, newArray, 0, superConf.length);
		newArray[superConf.length] = CoreOptions.composite(ogemaBundles());
		return newArray;
	}
	
	private volatile String user;
	private volatile Bundle userProxy;
	
	@Before
	public void createTestUser() throws InterruptedException, IOException {
		this.userProxy = newUser();
		this.user = userProxy.getLocation().substring("urp:".length());
	}
	
	@After
	public void removeUser() throws IOException, InvalidSyntaxException {
		if (user != null)
			removeUser(user, true);
		user = null;
		userProxy = null;
	}

	public Option[] ogemaBundles() {
		return new Option[] {
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.framework.security", "2.6.0"),
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
				CoreOptions.mavenBundle("org.ogema.ref-impl", "ogema-security-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "persistence").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "channel-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "hardware-manager").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "recordeddata-slotsdb").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "util").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "rest").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.tools", "resource-utils").version(OGEMA_VERSION),
				
				CoreOptions.mavenBundle("org.smartrplace.tools", "smartrplace-servlet-context", "0.0.1-SNAPSHOT").start(),
				
		};
	}
	
	private Bundle newUser() throws InterruptedException, IOException {
		return newUser(true);
	}
	
	private Bundle newUser(boolean createClient) throws InterruptedException, IOException {
		return createUserAndClient("testuser_" + userCnt.getAndIncrement(), 10000, createClient);
	}
	
	private Bundle createUserAndClient(String name, long timeout, boolean createClient) throws InterruptedException, IOException {
		Assert.assertTrue("User creation failed", permMan.getAccessManager().createUser(name, name, false));
		Bundle b = ctx.getBundle("urp:" + name);
		long start = System.currentTimeMillis();
		while (b == null && System.currentTimeMillis() - start <= timeout) {
			Thread.sleep(100);
			b = ctx.getBundle("urp:" + name);
		}
		if (!createClient)
			return b;
		Assert.assertNotNull("User bundle not found",b);
		final Bundle clientBundle = checkBundleStarted("org.smartrplace.tools.file-upload-client-v2");
		final org.osgi.service.cm.Configuration cfg = configAdmin.getConfiguration(FileUploadClient.CLIENT_PID, clientBundle.getLocation());
		final Dictionary<String, Object> clientProps=  new Hashtable<>();
		clientProps.put("remoteUrl", "http://localhost:" + HTTP_PORT + ServletConstants.DEFAULT_PATH_PREFIX + FileUploadConstants.PATH);
		clientProps.put("remoteUser", name);
		clientProps.put("remotePw", name);
		cfg.update(clientProps);
		return b;
	}
	
	private void removeUser(String name, boolean removeClientPid) throws IOException, InvalidSyntaxException {
		permMan.getAccessManager().removeUser(name);
		if (removeClientPid) {
			final Bundle clientBundle = checkBundleStarted("org.smartrplace.tools.file-upload-client-v2");
			final org.osgi.service.cm.Configuration[] configs = configAdmin.listConfigurations("(&(service.bundleLocation=" + clientBundle.getLocation() + ")(service.pid=" + FileUploadClient.CLIENT_PID + "))");
			if (configs != null)
				Arrays.stream(configs).forEach(cfg -> {
					try {
						cfg.delete();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
		}
	}
	
	private ServiceReference<FileUploadClient> waitForClient(long timeout) throws InterruptedException {
		long start = System.currentTimeMillis();
		ServiceReference<FileUploadClient> clientRef = ctx.getServiceReference(FileUploadClient.class);
		while (clientRef == null && System.currentTimeMillis() - start <= timeout) {
			Thread.sleep(100);
			clientRef = ctx.getServiceReference(FileUploadClient.class);
		}
		Assert.assertNotNull("Upload client service not available",clientRef);
		return clientRef;
	}
	
	private boolean addUserFilePermission(final Bundle bundle, final boolean includeWritePermission) {
		final String user = bundle.getLocation().substring("urp:".length());
		String path = uploadFolder.resolve(user).normalize().toString().replace('\\', '/');
		if (!path.startsWith(".") && !path.startsWith("/")) // relative FilePermission behaves super strange... we need to stick to a fixed convention for the paths
			path = "./" + path;
		return addFilePermission(bundle, path, includeWritePermission);
	}
	
	private boolean addFilePermission(final Bundle bundle, final String path, final boolean includeWritePermission) {
		final ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
		addPermission(bundle, FilePermission.class, path, includeWritePermission ? "read,write" : "read", cpa, cpu, true, -1);
		addPermission(bundle, FilePermission.class, path + "/-", includeWritePermission ? "read,write" : "read", cpa, cpu, true, -1);
		return cpu.commit();
	}
	
	private boolean addPermission(final Bundle bundle, final Class<? extends Permission> type, final String name, final String actions, final boolean allowOrDeny) {
		final ConditionalPermissionUpdate cpu = cpa.newConditionalPermissionUpdate();
		addPermission(bundle, type, name, actions, cpa, cpu, allowOrDeny, -1);
		return cpu.commit();
	}
	
	private static void addPermission(final Bundle bundle, final Class<? extends Permission> type, final String name, final String actions, 
			final ConditionalPermissionAdmin cpAdmin, final ConditionalPermissionUpdate update, final boolean allowOrDeny, int index) {
        List<ConditionalPermissionInfo> permissions = update.getConditionalPermissionInfos();
        if (index == -1) {
            index = permissions.size();
        }
		permissions.add(index,
				cpAdmin.newConditionalPermissionInfo(
						"testCond" + permissionCnt.getAndIncrement(), 
						new ConditionInfo[] {
			 					new ConditionInfo("org.osgi.service.condpermadmin.BundleLocationCondition", new String[]{bundle.getLocation()}) }, 
						new PermissionInfo[] {
							 new PermissionInfo(type.getName(), name, actions)}, 
						allowOrDeny ? "allow" : "deny"));
	}
	
	@Test
	public void secManIsAvailable() {
		Assert.assertNotNull("Security manager is null",System.getSecurityManager());
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
	public void multipleUserCreationWorks() throws InterruptedException, IOException, InvalidSyntaxException {
		Bundle newUser = null;
		try {
			newUser = newUser(false);
		} finally {
			if (newUser != null)
				removeUser(newUser.getLocation().substring("urp:".length()), false);
		}
	}
	
	@Test
	public void unauthorizedUserUploadWorks() throws Exception {
		final String test = "testString";
		final byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
		final InputStream stream = new ByteArrayInputStream(bytes);
		final FileConfiguration cfg = new FileConfiguration();
		cfg.filePrefix = nextFilePrefix();
		cfg.fileEnding = ".json";
		final HttpResponse response;
		final ServiceReference<FileUploadClient> clientRef = waitForClient(5000);
		final FileUploadClient client = ctx.getService(clientRef);
		try {
			final Future<HttpResponse> future = client.upload(stream, bytes.length, null, null, cfg);
			response = future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new AssertionError("file upload timed out");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			ctx.ungetService(clientRef);
		}
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 
				HttpServletResponse.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void authorizedUserUploadWorks() throws Exception {
		addUserFilePermission(userProxy, true);
		final String test = "testString";
		final byte[] bytes = test.getBytes(StandardCharsets.UTF_8);
		final InputStream stream = new ByteArrayInputStream(bytes);
		final FileConfiguration cfg = new FileConfiguration();
		cfg.filePrefix = nextFilePrefix();
		cfg.fileEnding = ".json";
		
		final HttpResponse response;
		final ServiceReference<FileUploadClient> clientRef = waitForClient(5000);
		final FileUploadClient client = ctx.getService(clientRef);
		try {
			final Future<HttpResponse> future = client.upload(stream, bytes.length, null, null, cfg);
			response = future.get(5, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new AssertionError("file upload timed out");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			ctx.ungetService(clientRef);
		}
		Assert.assertEquals("Unexpected file upload response: " + response.getStatusLine().getReasonPhrase(), 
				HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
	}
	

}
