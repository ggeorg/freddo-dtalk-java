/*
 * Copyright (c) 2013-2015 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
	public void contextInitialized(final ServletContextEvent sce) {
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
