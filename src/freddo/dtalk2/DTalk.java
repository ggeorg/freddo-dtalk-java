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
package freddo.dtalk2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.veracloud.jton.JtonObject;

import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.broker.netty.NettyBroker;
import freddo.dtalk2.client.ClientConnection;
import freddo.dtalk2.client.netty.NettyClient;
import freddo.dtalk2.discovery.ZeroconfService;
import freddo.dtalk2.discovery.jmdns.ZeroconfServiceImpl;
import freddo.dtalk2.messaging.Dispatcher;
import freddo.messagebus.MessageBus;
import freddo.messagebus.MessageBusListener;

public class DTalk {
	private static final Logger LOG = LoggerFactory.getLogger(DTalk.class);
	
	public static final String DTALKSRV_PATH = "/dtalksrv";
	public static final int VERSION = 1;
	
	public static final String DTALK_INBOUND_MSG = "dtalk.InboundMsg";
	public static final String DTALK_OUTBOUND_MSG = "dtalk.OutboundMsg";

	private static volatile DTalk sInstance = null;

	public static DTalk getInstance() {
		if (sInstance == null) {
			synchronized (DTalk.class) {
				if (sInstance == null) {
					sInstance = new DTalk();
				}
			}
		}
		return sInstance;
	}

	//
	// Messaging
	//

	public static void sendMessage(DTalkMessage message) {
		LOG.trace(">>> sendMessage: {}", message);
		
		DTalk dTalk = getInstance();
		if (!dTalk.mStarted) {
			LOG.warn("DTalk not started.");
			return;
		}
		
		String from = message.getFrom();
		if (from == null) {
			from = dTalk.getPublishedName();
		}
		
		String to = message.getTo();
		String service = message.getService();
		if ("dtalk.Dispatcher".equals(service)) {
			String action = message.getAction();
			if ("subscribe".equals(action)) {
				dTalk.mDispatcher.subscribe(message);
			} else if ("unsubscribe".equals(action)) {
				dTalk.mDispatcher.unsubscribe(message);
			}
			
			// Local dispatcher? If yes return.
			if (to == null || to.equals(dTalk.getPublishedName())) {
				return;
			}
		}
		
		if (to != null && !to.equals(dTalk.getPublishedName())) {
			LOG.debug("Outbound Message: {}", message);
			MessageBus.sendMessage(DTALK_OUTBOUND_MSG, message);
		} else {
			LOG.debug("Inbound Message: {}", message);
			MessageBus.sendMessage(DTALK_INBOUND_MSG, message);
		}
	}

	private static ScheduledExecutorService sScheduledExecutorService = null;

	private static ScheduledExecutorService ensureScheduledExecutorServiceExists() {
		if (sScheduledExecutorService == null) {
			sScheduledExecutorService = Executors.newScheduledThreadPool(5);
		}
		return sScheduledExecutorService;
	}

	@SuppressWarnings("deprecation")
	public static void sendRequest(DTalkRequest request) {
		LOG.trace(">>> sendRequest: {}", request);
		request.send(ensureScheduledExecutorServiceExists(), 33333L);
	}

	public static HandlerRegistration subscribe(final DTalkMessage.Handler handler) {
		MessageBus.subscribe(handler.getTopic(), handler);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				MessageBus.unsubscribe(handler.getTopic(), handler);
			}
		};
	}

	//
	// Internal Messaging
	//

	@Deprecated
	public static <T> void sendMessage(String topic, T message) {
		LOG.trace(">>> sendMessage: topic='{}', message='{}' (Thread: {})", topic, message, Thread.currentThread());
		MessageBus.sendMessage(topic, message);
	}

	@Deprecated
	public static <T> HandlerRegistration subscribe(final String topic, final MessageBusListener<T> listener) {
		MessageBus.subscribe(topic, listener);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				MessageBus.unsubscribe(topic, listener);
			}
		};
	}

	//
	// Life cycle
	//

	public static void start(DTalkConfiguration config) {
		getInstance().startImpl(config);
	}

	public static void shutdown() {
		getInstance().shutdownImpl();
	}

	//
	// Presence
	//
	
	public String getPublishedName() {
		return mZeroconfService != null ? mZeroconfService.getPublishedName() : null;
	}

	public static boolean hasPresence(String name) {
		Map<String, JtonObject> presences = getInstance().mPresenceMap;
		if (presences != null) {
			synchronized(presences) {
				return presences.containsKey(name);
			}
		}
		return false;
	}
	
	public static JtonObject getPresence(String name) {
		Map<String, JtonObject> presences = getInstance().mPresenceMap;
		if (presences != null) {
			synchronized(presences) {
				return presences.get(name);
			}
		}
		return null;
	}
	
	public static JtonObject addPresence(String name, JtonObject presence) {
		Map<String, JtonObject> presences = getInstance().mPresenceMap;
		if (presences != null) {
			synchronized(presences) {
				return presences.put(name, presence);
			}
		}
		return null;
	}
	
	public static JtonObject removePresence(String name) {
		Map<String, JtonObject> presences = getInstance().mPresenceMap;
		if (presences != null) {
			synchronized(presences) {
				return presences.remove(name);
			}
		}
		return null;
	}
	
	public static ClientConnection connectTo(String to)  {
		JtonObject presence = DTalk.getPresence(to);
		if (null != presence) {
			String host = presence.get("host").getAsString(null);
			if (host != null) {
				int port = presence.get("port").getAsInt(-1);
				if (port != -1) {
					StringBuilder sb = new StringBuilder();
					sb.append("ws://");
					sb.append(host).append(':').append(port);
					sb.append(DTalk.DTALKSRV_PATH);
					try {
						return new NettyClient(to, new URI(sb.toString()));
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return null;
	}

	//
	// Connection Registry
	//

	public static DTalkConnection addConnection(DTalkConnection connection) {
		LOG.trace(">>> addConnection: {}", null != connection ? connection.getName() : null);
		Map<String, DTalkConnection> conns = getInstance().mConnMap;
		if (conns != null) {
			synchronized (conns) {
				return conns.put(connection.getName(), connection);
			}
		}
		return null;
	}

	public static DTalkConnection getConnection(String target) {
		DTalk dTalk = getInstance();
		if (dTalk.mConnMap != null) {
			synchronized (dTalk.mConnMap) {
				return dTalk.mConnMap.get(target);
			}
		}
		return null;
	}

	// ---------------------------

	private boolean mStarted = false;
	private Dispatcher mDispatcher;
	private Broker mBroker = null;

	private Map<String, DTalkConnection> mConnMap;
	private Map<String, JtonObject> mPresenceMap;

	private ZeroconfService mZeroconfService;

	private DTalk() {
		// hidden
	}

	private void startImpl(DTalkConfiguration config) {
		LOG.info(">>> start: {}", config);

		if (mStarted) {
			LOG.warn("DTalk already started");
			return;
		}

		synchronized (this) {

			if (mStarted) {
				LOG.warn("DTalk already started");
				return;
			}

			mStarted = true;

			try {

				// Connections

				if (mConnMap != null) {
					for (DTalkConnection c : mConnMap.values()) {
						try {
							c.close();
						} catch (Exception e) {
							// ignore
						}
					}
					mConnMap.clear();
				}

				mConnMap = new ConcurrentHashMap<String, DTalkConnection>();
				
				// Presences
				
				if (mPresenceMap != null) {
					mPresenceMap.clear();
				}
				
				mPresenceMap = new ConcurrentHashMap<String, JtonObject>();

				// Dispatcher

				if (mDispatcher != null) {
					mDispatcher.shutdown();
					mDispatcher = null;
				}

				mDispatcher = new Dispatcher();
				mDispatcher.start();

				// Broker

				if (mBroker != null) {
					mBroker.shutdown();
					mBroker = null;
				}

				mBroker = config.getBroker();
				if (mBroker != null) {
					mBroker.initialize(config);
					mBroker.start();
				}

				// Discovery

				if (mZeroconfService != null) {
					mZeroconfService.shutdown();
					mZeroconfService = null;
				}

				mZeroconfService = config.getZeroconfService();
				if (mZeroconfService != null) {
					mZeroconfService.initialize(config);
					mZeroconfService.start();
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void shutdownImpl() {
		LOG.trace(">>> shutdown");

		if (!mStarted) {
			LOG.warn("DTalk not running");
			return;
		}

		synchronized (this) {

			if (!mStarted) {
				LOG.warn("DTalk not running");
				return;
			}

			mStarted = false;

			if (mZeroconfService != null) {
				try {
					mZeroconfService.shutdown();
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				} finally {
					mZeroconfService = null;
				}
			}

			try {
				mDispatcher.shutdown();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			} finally {
				mDispatcher = null;
			}

			if (mBroker != null) {
				try {
					mBroker.shutdown();
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				} finally {
					mBroker = null;
				}
			}
			
			// Connections

			if (mConnMap != null) {
				for (DTalkConnection c : mConnMap.values()) {
					try {
						c.close();
					} catch (Exception e) {
						// ignore
					}
				}
				mConnMap.clear();
			}

			mConnMap = new ConcurrentHashMap<String, DTalkConnection>();
			
			// Presences
			
			if (mPresenceMap != null) {
				mPresenceMap.clear();
			}
			
			mPresenceMap = new ConcurrentHashMap<String, JtonObject>();
		}

	}

	//
	// Main
	//

	public static void main(String[] args) {
		DTalk.start(new DTalkConfiguration() {

			@Override
			public int getPort() {
				return 8888;
			}

			@Override
			public InetAddress getAddress() {
				try {
					return InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					return null;
				}
			}
			
			@Override
			public Broker getBroker() {
				return new NettyBroker();
			}

			@Override
			public ZeroconfServiceImpl getZeroconfService() {
				return new ZeroconfServiceImpl();
			}

			@Override
			public String getServiceName() {
				return "MyService";
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

		LOG.debug("Done!!!");
	}

}
