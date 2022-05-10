package org.smartrplace.tools.servlets;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author jlapp
 */
@Designate(ocd=Redirect.Config.class, factory = true)
@Component(configurationPid = Redirect.PID,
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Redirect extends HttpServlet implements Servlet {
	
	public static final String PID = "org.smartplace.tools.RedirectServlet";
	private static final long serialVersionUID = 1L;
	
	@ObjectClassDefinition
	public static @interface Config {
		String[] osgi_http_whiteboard_servlet_pattern();
		String redirect();
	}
	
	Config cfg;
	
	@Activate
	void activate(Config cfg) {
		this.cfg = cfg;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.sendRedirect(cfg.redirect());
	}	
	
}
