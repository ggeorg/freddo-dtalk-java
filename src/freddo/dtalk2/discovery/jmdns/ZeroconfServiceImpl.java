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
package freddo.dtalk2.discovery.jmdns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConfiguration;
import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.discovery.MDNS;
import freddo.jmdns.JmDNS;
import freddo.jmdns.ServiceEvent;
import freddo.jmdns.ServiceInfo;
import freddo.jmdns.ServiceListener;

public class MDNSImpl extends MDNS implements ServiceListener {
	private static Logger LOG = LoggerFactory.getLogger(MDNSImpl.class);

	private static String SERVICE_TYPE = "_http._tcp.local.";

	private JmDNS mJmDNS = null;
	private String mServiceName;
	private int mPort;
	private InetAddress mAddress;
	private ServiceInfo mServiceInfo;
	
	public String getPublishedName() {
		return mServiceInfo != null ? mServiceInfo.getName() : null;
	}

	@Override
	public void initialize(DTalkConfiguration config) {
		synchronized (this) {
			if (mJmDNS == null) {
				try {
					mJmDNS = JmDNS.create("DTalk-JmDNS");
					// mJmDNS = JmDNS.create(config.getSocketAddress().getAddress(),
					// "DTalk-JmDNS");
					mServiceName = config.getServiceName();
					mPort = config.getSocketAddress().getPort();
				} catch (IOException e) {
					LOG.error("Error creating JmDNS", e);
					return;
				}
			}
		}
	}

	@Override
	public void start() {
		LOG.trace(">>> start");
		synchronized (this) {
			if (mJmDNS != null) {
				LOG.debug("Add service listener: {}", SERVICE_TYPE);
				mJmDNS.addServiceListener(SERVICE_TYPE, this);
				// ServiceInfo[] services = mJmDNS.list(SERVICE_TYPE);
				// LOG.debug("Existing: {}", services.length);
				// for (ServiceInfo s : services) {
				// LOG.debug("Existing: {}", s);
				// }
				
				Map<String, String> props = new HashMap<String, String>();
				props.put("dtalk", "1");
				props.put("dtype", "Test/1;");
				
				mServiceInfo = ServiceInfo.create(SERVICE_TYPE, mServiceName, mPort, 0, 0, props);
				
				try {
					mJmDNS.registerService(mServiceInfo);					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void onShutdown() {
		LOG.trace(">>> onShutdown");
		synchronized (this) {
			if (mJmDNS != null) {
				try {
					mJmDNS.unregisterAllServices();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					mJmDNS.removeServiceListener(SERVICE_TYPE, this);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					mJmDNS.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					mJmDNS = null;
				}
			}
		}
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		LOG.trace(">>> serviceAdded: {}", event.getName());
		mJmDNS.requestServiceInfo(event.getType(), event.getName());
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		LOG.trace(">>> serviceRemoved: {}", event.getName());
		final String name = event.getName();
		if (DTalk.hasPresence(name)) {
			JtonObject params = new JtonObject();
			params.add("name", name);
			params.add("type", event.getType());
			DTalk.fireEvent(this, "onremoved", params);
		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		LOG.trace(">>> serviceResolved: {}", event.getInfo());
		final String name = event.getName();
		ServiceInfo info = event.getInfo();
		if (info != null && !StringUtils.isEmpty(info.getServer())) {
			JtonObject params = new JtonObject();
			params.add("name", name);
			params.add("type", event.getType());
			params.add("host", info.getServer());
			params.add("port", info.getPort());
			params.add("local", info.equals(mServiceInfo));
			JtonObject props = new JtonObject();
			Enumeration<String> keys = info.getPropertyNames();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				props.add(key, info.getPropertyString(key));
			}
			params.add("properties", props);
			if (!DTalk.hasPresence(name)) {
				LOG.debug("RESOLVED: {}", params);
				DTalk.addPresence(name, params);
				DTalk.fireEvent(this, "onresolved", params);
			} else {
				DTalk.addPresence(name, params);
			}
		}
	}

	@SuppressWarnings("unused")
	private String getServer(ServiceInfo info) {
		String server = info.getServer();
		if (!StringUtils.isEmpty(server)) {
			return server;
		} else {
			InetAddress[] addresses = info.getInetAddresses();
			if (addresses.length > 0) {
				for (InetAddress address : addresses) {
					if (!address.isAnyLocalAddress()) {
						return address.getHostAddress();
					}
				}
			}
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		DTalk.start(new DTalkConfiguration() {
			@Override
			public Class<? extends Broker> getBrokerClass() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Class<? extends MDNSImpl> getMDNSClass() {
				return MDNSImpl.class;
			}

			@Override
			public InetSocketAddress getSocketAddress() {
				try {
					return new InetSocketAddress(InetAddress.getLocalHost(), 8888);
				} catch (UnknownHostException e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public String getServiceName() {
				return "GGEORG";
			}
		});

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			for (String input = br.readLine(); input != null;) {
				DTalk.shutdown();
				break;
			}

		} catch (IOException io) {
			io.printStackTrace();
		}
	}

}
