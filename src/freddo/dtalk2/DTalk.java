package freddo.dtalk;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk.broker.Broker;
import freddo.dtalk.broker.netty.NettyBroker;
import freddo.dtalk.util.MessageBus;

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

	// ---

	public static final String DTALKSRV_PATH = "/dtalksrv";

	private boolean mStarted = false;
	private MessageBus mMessageBus;
	private Broker mBroker = null;

	private DTalk() {
		// do nothing here
	}

	public void start(DTalkConfiguration config) {
		LOG.info(">>> start: {}", config);

		if (mStarted) {
			throw new IllegalStateException("DTalk already started");
		}

		synchronized (this) {

			if (mStarted) {
				throw new IllegalStateException("DTalk already started");
			}

			mStarted = true;
			mMessageBus = new MessageBus();

			try {
				
				// Dispatcher/router
				
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

	public void shutdown() {
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
				mBroker.shutdown();
				mBroker = null;
			}

			mMessageBus = null;
		}

	}

	//
	// Messaging
	//

	public void post(String topic, DTalkMessage message) {
		mMessageBus.sendMessage(topic, message);
	}

	//
	// Main
	//

	public static void main(String[] args) {
		DTalk.getInstance().start(new DTalkConfiguration() {
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
