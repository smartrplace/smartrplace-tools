package org.smartrplace.tools.upload.tests;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.smartrplace.tools.servlet.api.ServletAccessControl;
import org.smartrplace.tools.servlet.api.ServletConstants;

@RunWith(PaxExam.class)
public class SimpleContextTestBase extends TestBase {

	protected SimpleContextTestBase() {
		this(0, 0);
	}
	
	protected SimpleContextTestBase(long period, long delay) {
		super(period, delay);
	}
	
	private ServiceRegistration<ServletContextHelper> contextReg;
	private ServiceRegistration<ServletAccessControl> contextReg2;
	
	
	@Configuration
	public Option[] configuration() throws IOException {
		final Option[] superConf = super.configuration();
		final Option[] newArray = new Option[superConf.length+1];
		System.arraycopy(superConf, 0, newArray, 0, superConf.length);
		newArray[superConf.length] = CoreOptions.composite(contextBundles());
		return newArray;
	}
	
	public Option[] contextBundles() {
		return new Option[] {
			CoreOptions.wrappedBundle(CoreOptions.maven("org.smartrplace.tools", "smartrplace-servlet-api", "0.0.1"))
		};
	}
	
	@Before
	public void registerFilter() throws IOException, BundleException {
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
	
	public void startupWorks() throws IOException, InvalidSyntaxException, InterruptedException {
		checkBundleStarted("org.smartrplace.tools.file-upload-servlet-v2");
	}
	
}
