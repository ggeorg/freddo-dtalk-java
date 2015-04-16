package freddo.dtalk.broker.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DTalkContextListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(DTalkContextListener.class);
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.trace(">>> contextInitialized: {}", sce.getServletContext().getContextPath());
		
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.trace(">>> contextDestroyed: {}", sce.getServletContext().getContextPath());
		
	}

}
