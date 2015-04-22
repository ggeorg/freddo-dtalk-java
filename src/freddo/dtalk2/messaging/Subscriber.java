package freddo.dtalk2.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;

public class Subscriber implements DTalkMessage.Handler {
	private final static Logger LOG = LoggerFactory.getLogger(Subscriber.class);
	
	private volatile int mRefCnt = 1;

	private final String mTopic;
	private final String mTarget;

	private HandlerRegistration mHR = null;

	public Subscriber(String topic, String target) {
		mTopic = topic;
		mTarget = target;
		mHR = DTalk.subscribe(this);
	}

	@Override
	public String getTopic() {
		return mTopic;
	}

	int incRefCnt() {
		return ++mRefCnt;
	}
	
	int decRefCnt() {
		return --mRefCnt;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void messageSent(DTalkMessage message) {
		LOG.trace(">>> messageSend: {}", message);
		if (DTalk.getConnection(mTarget) != null) {
			OutboundMessage _message = new OutboundMessage(message);
			_message.setTo(mTarget);
			DTalk.sendMessage0(_message);
		} else {
			// TODO
		}
	}
	
	public void unsubscribe() {
		LOG.trace(">>> unsubscribe");
		if (mHR != null) {
			mHR.removeHandler();
			mHR = null;
		}
	}

}
