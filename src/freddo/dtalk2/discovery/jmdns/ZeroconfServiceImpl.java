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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.activemq.jmdns.JmDNS;
import org.apache.activemq.jmdns.ServiceEvent;
import org.apache.activemq.jmdns.ServiceInfo;
import org.apache.activemq.jmdns.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConfiguration;
import freddo.dtalk2.discovery.ZeroconfService;

public class ZeroconfServiceImpl extends ZeroconfService implements ServiceListener {
	private static Logger LOG = LoggerFactory.getLogger(ZeroconfServiceImpl.class);

	private static String SERVICE_TYPE = "_http._tcp.local.";

	protected JmDNS mJmDNS = null;
	private String mServiceName = null;
	private int mPort = 0;
	private InetAddress mAddress = null;
	private ServiceInfo mServiceInfo = null;
	
	public String getServiceName() {
		return mServiceName;
	}

	public void setServiceName(String serviceName) {
		mServiceName = serviceName;
	}

	public String getPublishedName() {
		return mServiceInfo != null ? mServiceInfo.getName() : null;
	}

	protected JmDNS createJmDNS() {
		if (mJmDNS == null) {
			try {
				mJmDNS = (mAddress != null) ? new JmDNS(mAddress) : new JmDNS();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return mJmDNS;
	}

	@Override
	public void initialize(DTalkConfiguration config) {
		synchronized (this) {
			mServiceName = config.getServiceName();
			mPort = config.getPort();
			mAddress = config.getAddress();
		}
	}

	@Override
	public void start() {
		LOG.trace(">>> start");
		synchronized (this) {
			if (mJmDNS == null) {
				JmDNS jmDNS = createJmDNS();
				if (jmDNS == null) {
					// TODO log
					return;
				}
				
				LOG.debug("Add service listener: {}", SERVICE_TYPE);
				jmDNS.addServiceListener(SERVICE_TYPE, this);
				// ServiceInfo[] services = mJmDNS.list(SERVICE_TYPE);
				// LOG.debug("Existing: {}", services.length);
				// for (ServiceInfo s : services) {
				// LOG.debug("Existing: {}", s);
				// }
				
				Hashtable<String, String> props = new Hashtable<String, String>();
				props.put("dtalk", "1");
				props.put("dtype", "Test/1;");

				mServiceInfo = new ServiceInfo(SERVICE_TYPE, mServiceName, mPort, 0, 0, props);

				try {
					jmDNS.registerService(mServiceInfo);
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
					mJmDNS.removeServiceListener(SERVICE_TYPE, this);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					mJmDNS.unregisterService(mServiceInfo);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					mServiceInfo = null;
				}
				
				try {
					mJmDNS.unregisterAllServices();
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
			fireEvent("onremoved", params);
		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		LOG.trace(">>> serviceResolved: {}", event.getInfo());
		final String name = event.getName();
		ServiceInfo info = event.getInfo();
		if (null != info && null != info.getServer()) {
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
				fireEvent("onresolved", params);
			} else {
				DTalk.addPresence(name, params);
			}
		}
	}

	@SuppressWarnings("unused")
	private String getServer(ServiceInfo info) {
		String server = info.getServer();
		if (null != server) {
			return server;
		} else {
			InetAddress address = info.getInetAddress();
			return address.getHostAddress();
		}
	}

}
