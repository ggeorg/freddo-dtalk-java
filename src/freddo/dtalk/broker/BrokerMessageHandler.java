package freddo.dtalk.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonElement;
import com.arkasoft.jton.JtonObject;
import com.arkasoft.jton.JtonParseException;
import com.arkasoft.jton.JtonParser;

import freddo.dtalk.DTalkConnection;
import freddo.dtalk.DTalkMessageHandler;

public final class BrokerMessageHandler implements DTalkMessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(BrokerMessageHandler.class);

	public void onMessage(DTalkConnection conn, String message) {
		try {
			JtonElement jtonElem = JtonParser.parse(message);
			if (jtonElem.isJtonObject()) {
				JtonObject _message = jtonElem.getAsJtonObject();
				int version = _message.get("dtalk").getAsInt();
				if (version == 2) {
					String topic = _message.has("topic") ? _message.get("topic").getAsString() : null;
					String action = _message.get("action").getAsString();
					if (topic == null) {
						if ("connect".equals(action)) {
							LOG.debug("Handle CONNECT ...");
							// TODO reply with OK or ERROR
						} else {
							// TODO if not authenticated reply with ERROR
							if ("disconnect".equals(action)) {
								LOG.debug("Handle DISCONNECT");
							}
						}
					} else {
						// TODO if not authenticated reply with ERROR
						
						
						
					}
				}
			}

			// message format error

		} catch (JtonParseException e) {

		} catch (UnsupportedOperationException e) {

		}
	}

}
