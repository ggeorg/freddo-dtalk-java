package freddo.dtalk2.broker.servlet;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 * An implementation of {@link ServerEndpointConfig.Configurator}.
 * <p>
 * 
 * 
 * @author ggeorg
 */
public class ServerEndpointConfigurator extends ServerEndpointConfig.Configurator {
	static final String DTALK_HANDSHAKE_REQUEST_KEY = "dtalk-handshake-req";

	@Override
	public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse resp) {
		super.modifyHandshake(conf, req, resp);
		conf.getUserProperties().put(DTALK_HANDSHAKE_REQUEST_KEY, req);
	}
}
