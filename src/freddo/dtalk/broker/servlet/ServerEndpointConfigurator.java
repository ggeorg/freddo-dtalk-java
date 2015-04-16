package freddo.dtalk.broker.servlet;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import freddo.dtalk.broker.BrokerMessageHandler;

/**
 * An implementation of {@link ServerEndpointConfig.Configurator}.
 * <p>
 * 
 * 
 * @author ggeorg
 */
public class ServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
	static final String DTALK_HANDSHAKE_REQUEST_KEY = "dtalk-handshake-req";
	static final String BROKER_MESSAGE_HANDLER_KEY = "broker-message_handler";

	@Override
	public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse resp) {
		super.modifyHandshake(conf, req, resp);
		conf.getUserProperties().put(DTALK_HANDSHAKE_REQUEST_KEY, req);
		conf.getUserProperties().put(BROKER_MESSAGE_HANDLER_KEY, new BrokerMessageHandler());
	}
}
