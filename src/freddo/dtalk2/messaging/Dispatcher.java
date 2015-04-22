package freddo.dtalk2.messaging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonElement;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;
import freddo.dtalk2.services.DTalkAction;
import freddo.dtalk2.services.DTalkService;

public class Dispatcher extends DTalkService {
	private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

	private HandlerRegistration mInboundMessageHR = null;
	private HandlerRegistration mOutboundMessageHR = null;

	private final Map<String, Subscriber> mSubscribers;

	public Dispatcher() {
		super("dtalk.service.Dispatcher");
		mSubscribers = new ConcurrentHashMap<String, Subscriber>();
	}

	public void start() {
		removeHandlers();
		addHandlers();
	}

	@Override
	protected void onShutdown() {
		removeHandlers();
		mSubscribers.clear();
	}

	@DTalkAction(name = "subscribe")
	public void subscribe(DTalkMessage message) {
		LOG.trace(">>> doSubscribe: {}", message);
		JtonElement params = message.getParams();
		if (params != null) {
			if (params.isJtonPrimitive()) {
				String topic = params.getAsString(null);
				if (topic != null) {
					String regKey = topic;
					if (message instanceof InboundMessage) {
						InboundMessage _message = (InboundMessage) message;
						@SuppressWarnings("deprecation")
						String connection = _message.getConnection().getId();
						String from = message.getFrom();
						if (from != null && DTalk.hasPresence(from)) {
							regKey += ("@" + from);
							subscribe(regKey, topic, from);
						} else {
							regKey += ("@" + connection);
							subscribe(regKey, topic, connection);
						}

						// TODO return...
						DTalk.fireEvent(this, "onsubscribe", params);
					}
				}
			}
		}
	}

	private void subscribe(String regKey, String topic, String target) {
		synchronized (mSubscribers) {
			if (!mSubscribers.containsKey(regKey)) {
				LOG.debug("New subscription: {}", regKey);
				mSubscribers.put(regKey, new Subscriber(topic, target));
			} else {
				LOG.debug("Increase refCnt: {}", regKey);
				mSubscribers.get(regKey).incRefCnt();
			}
		}
	}
	
	@DTalkAction(name = "unsubscribe")
	public void unsubscribe(DTalkMessage message) {
		LOG.trace(">>> unsubscribe: {}", message);
		JtonElement params = message.getParams();
		if (params != null) {
			if (params.isJtonPrimitive()) {
				String topic = params.getAsString(null);
				if (topic != null) {
					String regKey = topic;
					if (message instanceof InboundMessage) {
						InboundMessage _message = (InboundMessage) message;
						@SuppressWarnings("deprecation")
						String connection = _message.getConnection().getId();
						String from = message.getFrom();
						if (from != null && DTalk.hasPresence(from)) {
							regKey += ("@" + from);
							unsubscribe(regKey);
						} else {
							regKey += ("@" + connection);
							unsubscribe(regKey);
						}

						// TODO return...
					}
				}
			}
		}
	}

	private void unsubscribe(String regKey) {
		synchronized (mSubscribers) {
			Subscriber subscriber = mSubscribers.get(regKey);
			if (subscriber != null) {
				if (subscriber.decRefCnt() == 0) {
					LOG.debug("Remove subscription: {}", regKey);
					subscriber.unsubscribe();
					mSubscribers.remove(regKey);
				} else {
					LOG.debug("Decrease refCnt: {}", regKey);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void addHandlers() {
		mInboundMessageHR = DTalk.subscribe0(InboundMessage.class, new InboundMessageHandler());
		mOutboundMessageHR = DTalk.subscribe0(OutboundMessage.class, new OutboundMessageHandler());
	}

	private void removeHandlers() {
		if (mInboundMessageHR != null) {
			mInboundMessageHR.removeHandler();
			mInboundMessageHR = null;
		}
		if (mOutboundMessageHR != null) {
			mOutboundMessageHR.removeHandler();
			mOutboundMessageHR = null;
		}
	}

}
