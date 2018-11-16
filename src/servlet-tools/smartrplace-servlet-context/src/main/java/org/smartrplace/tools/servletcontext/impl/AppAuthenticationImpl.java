package org.smartrplace.tools.servletcontext.impl;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.lang3.RandomStringUtils;
import org.ogema.accesscontrol.RestAccess;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.smartrplace.tools.servlet.api.AppAuthentication;
import org.smartrplace.tools.servlet.api.ServletConstants;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component(
		service=Application.class,
		configurationPid=AppAuthenticationImpl.PID,
		configurationPolicy=ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=AppAuthenticationImpl.Config.class)
public class AppAuthenticationImpl extends HttpServlet implements AppAuthentication, Application {

	public static final String PID = "ogr.smartrplace.tools.servletcontext.AppAuthentication";
	private static final long serialVersionUID = 1L;
	private int nrChars = 24;

	@ObjectClassDefinition
	static @interface Config {
		
		@AttributeDefinition(description="Password size/nr characters", defaultValue="24")
		int pwChars() default 24;
		
	}
	

	@Reference
    private ComponentServiceObjects<RestAccess> restAccessService;
	
	private static final String ATTRIBUTE = "org.smartrplace.tools.acccontext";
	private final Cache<String, Context> cache = CacheBuilder.newBuilder()
			.weakValues()
			.build();
	private final SecureRandom rand = new SecureRandom();
	
	private BundleContext ctx;
	private ServiceRegistration<AppAuthentication> ownReg;
	private ApplicationManager appMan;
	
	protected void activate(BundleContext ctx, Config config) {
		this.ctx = ctx;
		this.nrChars = config.pwChars() > 8 ? config.pwChars() : 24;
	}
	
	@Override
	public void start(ApplicationManager appManager) {
		this.appMan = appManager;
		appManager.getWebAccessManager().registerWebResource(ServletConstants.APP_AUTH_SERVLET, this);
		this.ownReg = ctx.registerService(AppAuthentication.class, this, null);
	}
	
	@Override
	public void stop(AppStopReason reason) {
		try {
			appMan.getWebAccessManager().unregisterWebResource(ServletConstants.APP_AUTH_SERVLET);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ForkJoinPool.commonPool().submit(() -> {
			try {
				ownReg.unregister();
			} catch (Exception ignore) {}
		});
		cache.invalidateAll();
	}

	@Override
	public AccessControlContext getContext(char[] token) {
		final String str = new String(token);
		final Context c = cache.getIfPresent(str);
		if (c == null)
			return null;
		return c.accs.get(str);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final HttpSession session = req.getSession(false);
		if (session == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		final RestAccess ra = restAccessService.getService();
		final AccessControlContext ctx;
		try {
			ctx = ra.getAccessContext(req, resp);
		} finally {
			restAccessService.ungetService(ra);
		}
		if (ctx == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		Context value = null;
		try {
			value = (Context) session.getAttribute(ATTRIBUTE);
		} catch (ClassCastException expected) {} // happens when the bundle gets updated
		if (value == null) {
			value = new Context();
			session.setAttribute(ATTRIBUTE, value);
		}
		String r;
		while (true) {
			r = RandomStringUtils.random(nrChars, 0, 0, true, true, null, rand);
			final Context old = cache.asMap().putIfAbsent(r, value);
			if (old == null)
				break;
		}
		value.accs.put(r, ctx);
		resp.getWriter().write(r);
		resp.setContentType("text/plain");
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	

	private static class Context implements HttpSessionBindingListener {
		
		final Map<String, AccessControlContext> accs = new ConcurrentHashMap<>(8);
		
		@Override
		public void valueBound(HttpSessionBindingEvent event) {
		}

		@Override
		public void valueUnbound(HttpSessionBindingEvent event) {
			accs.clear();
		}
		
	}

}
