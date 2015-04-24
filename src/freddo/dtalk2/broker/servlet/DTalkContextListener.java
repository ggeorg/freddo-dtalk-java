package freddo.dtalk2.broker.servlet;

import java.net.InetSocketAddress;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConfiguration;
import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.discovery.MDNS;

public abstract class DTalkContextListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(DTalkContextListener.class);
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.trace(">>> contextInitialized: {}", sce.getServletContext().getContextPath());
		
		DTalk.start(new DTalkConfiguration() {
			@Override
			public Class<? extends Broker> getBrokerClass() {
				return null;
			}

			@Override
			public Class<? extends MDNS> getMDNSClass() {
				return null;
			}

			@Override
			public InetSocketAddress getSocketAddress() {
				return new InetSocketAddress("localhost", 8888);
			}

			@Override
			public String getServiceName() {
				// TODO Auto-generated method stub
				return null;
			}
		});
		
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.trace(">>> contextDestroyed: {}", sce.getServletContext().getContextPath());
		DTalk.shutdown();
	}

}
