package freddo.dtalk2.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConnection;
import freddo.messagebus.MessageBusListener;

class OutboundMessageHandler implements MessageBusListener<OutboundMessage> {
	private static final Logger LOG = LoggerFactory.getLogger(OutboundMessageHandler.class);

	@Override
	public void messageSent(OutboundMessage message) {
		LOG.trace(">>> messageSent: {}", message);
		
		DTalkConnection conn = DTalk.getConnection(message.getTo());
		if (conn != null) {
			//message.setFrom(DTalk.getLocal());
			conn.sendMessage(message.toString());
		}
		
	}

}
