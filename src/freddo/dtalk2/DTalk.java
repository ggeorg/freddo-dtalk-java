package freddo.dtalk2;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonElement;

import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.broker.netty.NettyBroker;
import freddo.dtalk2.messaging.Dispatcher;
import freddo.dtalk2.services.DTalkService;
import freddo.messagebus.MessageBus;
import freddo.messagebus.MessageBusListener;

public class DTalk {
	private static final Logger LOG = LoggerFactory.getLogger(DTalk.class);

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
		getInstance().mMessageBus.sendMessage(message.getTopic(), message);
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
		_response.setTopic(message.getId());
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
		_response.setTopic(sb.toString());
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
	public static <T> void sendMessage0(T message) {
		LOG.trace(">>> sendMessage: {} ({})", message, Thread.currentThread());
		getInstance().mMessageBus.sendMessage(message);
	}

	@Deprecated
	public static <T> void sendMessage0(String topic, T message) {
		LOG.trace(">>> sendMessage: {}->{} ({})", topic, message, Thread.currentThread());
		getInstance().mMessageBus.sendMessage(message);
	}

	@Deprecated
	public static <T> HandlerRegistration subscribe0(final Class<? super T> topic, final MessageBusListener<T> listener) {
		return subscribe0(topic.getName(), listener);
	}

	@Deprecated
	public static <T> HandlerRegistration subscribe0(final String topic, final MessageBusListener<T> listener) {
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

	public static boolean isLocal(String serviceName) {
		return false;
	}

	public static boolean hasPresence(String serviceName) {
		return false;
	}

	//
	// Connections
	//

	public static DTalkConnection addConnection(String target, DTalkConnection connection) {
		Map<String, DTalkConnection> conns = getInstance().mConnMap;
		if (conns != null) {
			synchronized (conns) {
				return conns.put(target, connection);
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

				// Dispatcher

				if (mDispatcher != null) {
					mDispatcher.shutdown();
					mDispatcher = null;
				}

				mDispatcher = new Dispatcher();
				mDispatcher.start();

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

				// Broker

				if (mBroker != null) {
					mBroker.shutdown();
					mBroker = null;
				}

				Class<? extends Broker> brokerCls = config.getBrokerClass();
				if (brokerCls != null) {
					mBroker = config.getBrokerClass().newInstance();
					mBroker.initialize(config);
					mBroker.start();
				}

				// Publish

				// Discovery

			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
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

			if (mBroker != null) {
				try {
					mBroker.shutdown();
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				} finally {
					mBroker = null;
				}
			}

			try {
				mDispatcher.shutdown();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			} finally {
				mDispatcher = null;
			}

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
			public InetSocketAddress getSocketAddress() {
				return new InetSocketAddress("localhost", 8888);
			}
		});

		LOG.debug("OK");
	}

}
