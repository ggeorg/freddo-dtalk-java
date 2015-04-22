package freddo.dtalk2.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalk;
import freddo.messagebus.MessageBusListener;

class InboundMessageHandler implements MessageBusListener<InboundMessage> {
	private static final Logger LOG = LoggerFactory.getLogger(InboundMessageHandler.class);

	@SuppressWarnings("deprecation")
	@Override
	public void messageSent(InboundMessage message) {
		LOG.trace(">>> messageSent: {}", message);

		String to = message.getFrom();
		if (to == null || DTalk.getInstance().isLocal(to)) {
			DTalk.sendMessage(message);
		} else {
			DTalk.sendMessage0(new OutboundMessage(message));
		}
	}

}
