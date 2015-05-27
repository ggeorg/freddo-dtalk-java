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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonElement;
import com.arkasoft.jton.JtonObject;

import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.broker.netty.NettyBroker;
import freddo.dtalk2.discovery.MDNS;
import freddo.dtalk2.discovery.jmdns.MDNSImpl;
import freddo.dtalk2.messaging.Dispatcher;
import freddo.dtalk2.services.DTalkService;
import freddo.messagebus.MessageBus;
import freddo.messagebus.MessageBusListener;

public class DTalk {
	private static final Logger LOG = LoggerFactory.getLogger(DTalk.class);
	
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
			dTalk.mMessageBus.sendMessage(DTALK_OUTBOUND_MSG, message);
		} else {
			LOG.debug("Inbound Message: {}", message);
			dTalk.mMessageBus.sendMessage(DTALK_INBOUND_MSG, message);
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

	public static void sendResponse(DTalkMessage message, JtonElement response) {
		LOG.trace(">>> sendResponse: {}", message);
		DTalkMessage _response = new DTalkMessage();
		_response.setVersion(DTalk.VERSION);
		_response.setService(message.getId());
		_response.setResult(response);
		String from = message.getFrom();
		if (from != null) {
			_response.setFrom(from);
		}
		sendMessage(_response);
	}

	public static void fireEvent(DTalkService source, String type, JtonElement params) {
		LOG.trace(">>> fireEvent: {}", type);
		StringBuilder sb = new StringBuilder();
		sb.append('$').append(source.getTopic()).append('#').append(type);
		DTalkMessage _response = new DTalkMessage();
		_response.setVersion(DTalk.VERSION);
		_response.setService(sb.toString());
		_response.setParams(params);
		sendMessage(_response);
	}

	public static HandlerRegistration subscribe(final DTalkMessage.Handler handler) {
		getInstance().mMessageBus.subscribe(handler.getTopic(), handler);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				getInstance().mMessageBus.unsubscribe(handler.getTopic(), handler);
			}
		};
	}

	//
	// Internal Messaging
	//

	@Deprecated
	public static <T> void sendMessage(String topic, T message) {
		LOG.trace(">>> sendMessage: {}->{} ({})", topic, message, Thread.currentThread());
		getInstance().mMessageBus.sendMessage(message);
	}

	@Deprecated
	public static <T> HandlerRegistration subscribe(final String topic, final MessageBusListener<T> listener) {
		getInstance().mMessageBus.subscribe(topic, listener);
		return new HandlerRegistration() {
			@Override
			public void removeHandler() {
				getInstance().mMessageBus.unsubscribe(topic, listener);
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
		return mMDNSService != null ? mMDNSService.getPublishedName() : null;
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

	//
	// Connections
	//

	public static DTalkConnection addConnection(DTalkConnection connection) {
		Map<String, DTalkConnection> conns = getInstance().mConnMap;
		if (conns != null) {
			synchronized (conns) {
				return conns.put(connection.getName(), connection);
			}
		}
		return null;
	}

	public static DTalkConnection getConnection(String target) {
		Map<String, DTalkConnection> conns = getInstance().mConnMap;
		if (conns != null) {
			synchronized (conns) {
				return conns.get(target);
			}
		}
		return null;
	}

	// ---------------------------

	public static final String DTALKSRV_PATH = "/dtalk2";

	public static final int VERSION = 2;

	private boolean mStarted = false;
	private MessageBus mMessageBus;
	private Dispatcher mDispatcher;
	private Broker mBroker = null;

	private Map<String, DTalkConnection> mConnMap;
	private Map<String, JtonObject> mPresenceMap;

	private MDNS mMDNSService;

	private DTalk() {
		// hidden
	}

	private void startImpl(DTalkConfiguration config) {
		LOG.info(">>> start: {}", config);

		if (mStarted) {
			throw new IllegalStateException("DTalk already started");
		}

		synchronized (this) {

			if (mStarted) {
				throw new IllegalStateException("DTalk already started");
			}

			mStarted = true;

			try {

				// Message Bus

				mMessageBus = new MessageBus();

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

				Class<? extends Broker> brokerCls = config.getBrokerClass();
				if (brokerCls != null) {
					mBroker = brokerCls.newInstance();
					mBroker.initialize(config);
					mBroker.start();
				}

				// Discovery

				if (mMDNSService != null) {
					mMDNSService.shutdown();
					mMDNSService = null;
				}

				Class<? extends MDNS> mDNSCls = config.getMDNSClass();
				if (mDNSCls != null) {
					mMDNSService = mDNSCls.newInstance();
					mMDNSService.initialize(config);
					mMDNSService.start();
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void shutdownImpl() {
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

			try {
				mMDNSService.shutdown();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			} finally {
				mMDNSService = null;
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

			// Message Bus
			
			mMessageBus = null;
		}

	}

	//
	// Main
	//

	public static void main(String[] args) {
		DTalk.start(new DTalkConfiguration() {
			@Override
			public Class<? extends Broker> getBrokerClass() {
				return NettyBroker.class;
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
					return null;
				}
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
