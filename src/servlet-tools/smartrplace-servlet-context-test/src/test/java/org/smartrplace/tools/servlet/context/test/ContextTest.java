package org.smartrplace.tools.servlet.context.test;

import java.io.FilePermission;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogema.accesscontrol.PermissionManager;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.smartrplace.tools.servlet.api.ServletAccessControl;
import org.smartrplace.tools.servlet.api.ServletConstants;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

@ExamReactorStrategy(PerClass.class)
@RunWith(PaxExam.class)
public class ContextTest {

	private static final String SLF4J_VERSION = "1.7.25";
	private static final String OGEMA_VERSION = "2.2.0";
	private static final Path osgiStorage = Paths.get("data/osgi-storage");
	private final static AtomicInteger permissionCnt = new AtomicInteger(0);
	private final static AtomicInteger userCnt = new AtomicInteger(0);
	private final static String ADDRESS = "/test";
	private static final String TEST_FILE = "./config/all.policy";
	private static final int HTTP_PORT = 4523;
	
	@Inject
	private BundleContext ctx;
	
	@Inject
	private ServletAccessControl accessControl;

	@Inject
	private ConditionalPermissionAdmin cpa;
	
	@Inject
	private PermissionManager permMan;
	
	private TestServlet servlet;
	private ServiceRegistration<Servlet> servletReg;
	
	@Before
	public void registerServlet() {
		this.servlet = new TestServlet();
		final Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, ADDRESS + "/*");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, ServletConstants.CONTEXT_FILTER);
		this.servletReg = ctx.registerService(Servlet.class, servlet, properties);
	}
	
	@After
	public void unregisterServlet() {
		this.servlet = null;
		final ServiceRegistration<Servlet> servletReg = this.servletReg;
		this.servletReg = null;
		if (servletReg != null) {
			servletReg.unregister();
		}
	}
	
	private Bundle newUser(boolean privileged) throws InterruptedException, IOException {
		return createUser("testuser_" + userCnt.getAndIncrement(), 5000, privileged);
	}
	
	private Bundle createUser(String name, long timeout, boolean privileged) throws InterruptedException, IOException {
		Assert.assertTrue("User creation failed", permMan.getAccessManager().createUser(name, name, false));
		Bundle b = ctx.getBundle("urp:" + name);
		long start = System.currentTimeMillis();
		while (b == null && System.currentTimeMillis() - start <= timeout) {
			Thread.sleep(100);
			b = ctx.getBundle("urp:" + name);
		}
		Assert.assertNotNull("User proxy not found", b);
		if (privileged) {
			Assert.assertTrue("Failed to add AllPermission for user",addPermission(b, AllPermission.class, "*", "*", true));
		}
		return b;
	}
	
	private void removeUser(String name) {
		permMan.getAccessManager().removeUser(name);
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
	
	private static String getUserName(final Bundle userBundle) {
		return userBundle.getLocation().substring("urp:".length());
	}
	
	private static int getJavaVersion() {
		String version = System.getProperty("java.specification.version");
		final int idx = version.indexOf('.');
		if (idx > 0)
			version = version.substring(idx + 1);
		return Integer.parseInt(version); 
	}
	
	private static URI getUrl(final String user, final Map<String,String> params) {
		try {
			final URIBuilder builder = new URIBuilder()
					.setScheme("http")
					.setHost("localhost")
					.setPort(HTTP_PORT)
					.setPath(ServletConstants.DEFAULT_PATH_PREFIX + ADDRESS)
					.addParameter("user", user)
					.addParameter("pw", user);
			if (params != null)
				params.forEach((key,value) -> builder.addParameter(key, value));
			return builder.build();
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}
	
	private static URI getUrl(final String user) {
		return getUrl(user, null);
	}

	private static HttpResponse send(Request request) throws ClientProtocolException, IOException {
		return request.connectTimeout(5000).socketTimeout(5000).execute().returnResponse();
	}
	
	@Configuration
	public Option[] configuration() throws IOException {
		return new Option[] {
				CoreOptions.cleanCaches(),
				CoreOptions.frameworkProperty("org.osgi.service.http.port").value(Integer.toString(HTTP_PORT)),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_SECURITY).value(Constants.FRAMEWORK_SECURITY_OSGI),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_STORAGE).value(osgiStorage.toString()), 
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_STORAGE_CLEAN).value(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT),
				CoreOptions.frameworkProperty(Constants.FRAMEWORK_BSNVERSION).value(Constants.FRAMEWORK_BSNVERSION_MULTIPLE),
				CoreOptions.vmOption("-ea"), 
				// these four options are required with the forked launcher; otherwise they are in the surefire plugin
				CoreOptions.vmOption("-Djava.security.policy=config/all.policy"),
				CoreOptions.vmOption("-Dorg.ogema.security=on"),
				CoreOptions.when(getJavaVersion() >= 9).useOptions(
					CoreOptions.vmOption("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"),
					CoreOptions.vmOption("--add-modules=java.xml.bind,java.xml.ws.annotation")
				),
				//
				CoreOptions.junitBundles(),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.framework.security", "2.6.1"),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "permission-admin").version(OGEMA_VERSION).startLevel(1),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.1.6"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.9.4"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin.filestore", "1.0.2"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.useradmin", "1.0.3"),
				CoreOptions.mavenBundle("org.osgi", "org.osgi.service.useradmin", "1.1.0"),
				
				// Jetty
				CoreOptions.mavenBundle("org.eclipse.jetty", "jetty-servlets", "9.4.11.v20180605"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.2"),
				CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "4.0.4").start(),
				
				// slf4j
				CoreOptions.mavenBundle("org.slf4j", "slf4j-api", SLF4J_VERSION),
				CoreOptions.mavenBundle("org.slf4j", "osgi-over-slf4j", SLF4J_VERSION),
				CoreOptions.mavenBundle("org.slf4j", "slf4j-simple", SLF4J_VERSION).noStart(),
				
				// Jackson
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-core", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-annotations", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.core", "jackson-databind", "2.9.6"),
				CoreOptions.mavenBundle("com.fasterxml.jackson.module", "jackson-module-jaxb-annotations", "2.9.6"),
				
				// commons
				CoreOptions.mavenBundle("commons-io", "commons-io", "2.6"),
				CoreOptions.mavenBundle("org.apache.commons", "commons-math3", "3.6.1"),
				CoreOptions.mavenBundle("commons-codec", "commons-codec", "1.11"),
				CoreOptions.mavenBundle("org.apache.commons", "commons-lang3", "3.7"),
				CoreOptions.mavenBundle("org.json", "json", "20170516"),
				CoreOptions.mavenBundle("com.google.guava", "guava", "23.0"),
				
				// OGEMA
				CoreOptions.mavenBundle("org.ogema.core", "api", OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.core", "models").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.core", "api").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.tools", "memory-timeseries").version(OGEMA_VERSION),
				CoreOptions.mavenBundle("org.ogema.ref-impl", "administration").version(OGEMA_VERSION),
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
				
				CoreOptions.mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "3.0.0"),
				CoreOptions.mavenBundle("biz.aQute.bnd", "biz.aQute.bndlib", "3.5.0"),
				
				CoreOptions.mavenBundle("org.smartrplace.tools", "smartrplace-servlet-context", "0.0.1"),
				CoreOptions.mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.5.6"),
				CoreOptions.mavenBundle("org.apache.httpcomponents", "httpcore-osgi","4.4.10"), 
				CoreOptions.mavenBundle("commons-logging", "commons-logging", "1.1.3") 
			};
	}
	
	@Test
	public void startupWorks() {
		Assert.assertNotNull("Security manager is null", System.getSecurityManager());
	}
	
	@Test
	public void servletIsAvailable() throws InterruptedException, IOException {
		final Bundle admin = newUser(true);
		final String name = getUserName(admin);
		try {
			final HttpResponse resp = send(Request.Get(getUrl(name)));
			Assert.assertEquals("Unexpected response",HttpServletResponse.SC_OK, resp.getStatusLine().getStatusCode());
			Assert.assertEquals(name, servlet.lastUser);
		} finally {
			removeUser(name);
		}
	}

	@Test
	public void validCredentialsRequired() throws InterruptedException, IOException {
		final HttpResponse resp = send(Request.Get(getUrl("test")));
		Assert.assertEquals("Unexpected response",HttpServletResponse.SC_UNAUTHORIZED, resp.getStatusLine().getStatusCode());
	}

	@Test
	public void permissionRequiredForServletAction() throws InterruptedException, IOException {
		final Bundle unprivileged = newUser(false);
		final String name = getUserName(unprivileged);
		try {
			final HttpResponse resp = send(Request.Get(getUrl(name, Collections.singletonMap("target", "readfile"))));
			Assert.assertEquals("Unexpected response",HttpServletResponse.SC_OK, resp.getStatusLine().getStatusCode());
			Assert.assertEquals(name, servlet.lastUser);
			Assert.assertNotNull("File reading succeeded despite missing permission", servlet.lastException);
			Assert.assertTrue("Not a security exception",servlet.lastException instanceof SecurityException);
		} finally {
			removeUser(name);
		}
	}
	
	@Test
	public void fileReadingSucceedsWithPermission() throws InterruptedException, IOException {
		final Bundle privileged = newUser(false);
		final String name = getUserName(privileged);
		addPermission(privileged, FilePermission.class, TEST_FILE, "read", true);
		try {
			final HttpResponse resp = send(Request.Get(getUrl(name, Collections.singletonMap("target", "readfile"))));
			Assert.assertEquals("Unexpected response",HttpServletResponse.SC_OK, resp.getStatusLine().getStatusCode());
			Assert.assertEquals(name, servlet.lastUser);
			Assert.assertNull("File reading failed despite permission", servlet.lastException);
		} finally {
			removeUser(name);
		}
	}
	
	@SuppressWarnings("serial")
	private class TestServlet extends HttpServlet {
		
		volatile CountDownLatch getLatch = new CountDownLatch(1);
		volatile String lastUser = null;
		volatile Exception lastException = null;
		
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			lastUser = (String) req.getAttribute(ServletContextHelper.REMOTE_USER);
			final String target = req.getParameter("target");
			if (target != null) {
				final Callable<String> task;
				switch (target.toLowerCase()) {
				case "readfile":
					task = () -> {
						try {
							return Files.readAllLines(Paths.get(TEST_FILE), StandardCharsets.UTF_8)
									.stream()
									.collect(Collectors.joining());
						} catch (IOException e1) {
							throw new UncheckedIOException(e1);
						}
					};
					break;
				default:
					task = null;
				}
				if (task != null) {
					final String response = AccessController.doPrivileged(new PrivilegedAction<String>() {
		
						@Override
						public String run() {
							try {
								return task.call();
							} catch (Exception e) {
								lastException = e;
								return "";
							}
						}
					}, accessControl.getAccessControlContext());
					resp.getWriter().write(response);
				}
			}
			resp.setStatus(HttpServletResponse.SC_OK);
			getLatch.countDown();
		}
		
	}
	
}
