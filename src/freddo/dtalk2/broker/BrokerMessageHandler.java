package freddo.dtalk2.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonArray;
import com.arkasoft.jton.JtonElement;
import com.arkasoft.jton.JtonParser;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConnection;
import freddo.dtalk2.messaging.InboundMessage;

public final class BrokerMessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(BrokerMessageHandler.class);

	public static void onMessage(DTalkConnection conn, String message) {
		try {
			JtonElement jtonElem = JtonParser.parse(message);
			if (jtonElem.isJtonObject()) {
				onMessage(new InboundMessage(conn, jtonElem.getAsJtonObject()));
			} else if (jtonElem.isJtonArray()) {
				onMessage(conn, jtonElem.getAsJtonArray());
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private static void onMessage(DTalkConnection conn, JtonArray jtonArray) {
		for (JtonElement jtonElem_i : jtonArray.getAsJtonArray()) {
			if (jtonElem_i.isJtonObject()) {
				onMessage(new InboundMessage(conn, jtonElem_i.getAsJtonObject()));
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static void onMessage(InboundMessage message) {
		LOG.trace(">>> onMessage: {}", message);
		try {
			int version = message.getVersion();
			if (version == 2) {
				String topic = message.getTopic();
				String action = message.getAction();
				if (topic == null) {
					if ("connect".equals(action)) {
						LOG.debug("Handle CONNECT");
						String regKey = message.getFrom();
						if (regKey == null) {
							regKey = message.getConnection().getId();
						}
						DTalk.addConnection(regKey, message.getConnection());
						// TODO reply with OK or ERROR
					} else {
						// TODO if not authenticated reply with ERROR
						if ("disconnect".equals(action)) {
							LOG.debug("Handle DISCONNECT");
							// TODO ???
						}
					}
				} else {
					DTalk.sendMessage0(message);
				}
			} else {
				// TODO dtalk != 2 not supported
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private BrokerMessageHandler() {
		// hidden
	}

}
