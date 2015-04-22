package freddo.dtalk.broker.servlet;

import static freddo.dtalk.broker.servlet.ServerEndpointConfigurator.BROKER_MESSAGE_HANDLER_KEY;
import static freddo.dtalk.broker.servlet.ServerEndpointConfigurator.DTALK_HANDSHAKE_REQUEST_KEY;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.Future;

import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk.DTalkConnection;
import freddo.dtalk.broker.BrokerMessageHandler;

@ServerEndpoint(value = "/dtalksrv", configurator = ServerEndpointConfigurator.class)
public class DTalkServerEndpoint implements DTalkConnection {
	private static final Logger LOG = LoggerFactory.getLogger(DTalkServerEndpoint.class);

	private EndpointConfig mConfig;
	private Session mSession;

	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		LOG.trace(">>> onOpen: {}, userProperties: {}", session.getId(), config.getUserProperties());

		mConfig = config;
		mSession = session;

		if (LOG.isDebugEnabled()) {
			HandshakeRequest req = getHandshakeRequest();
			HttpSession httpSession = (HttpSession) req.getHttpSession();
			LOG.debug("=================================");
			LOG.debug("QueryString   : {}", req.getQueryString());
			LOG.debug("RequestURI    : {}", req.getRequestURI());
			LOG.debug("Headers       : {}", req.getHeaders());
			LOG.debug("UserPrincipal : {}", req.getUserPrincipal());
			LOG.debug("ParameterMap  : {}", req.getParameterMap());
			LOG.debug("=================================");
			if (httpSession != null) {
				Enumeration<String> e = httpSession.getAttributeNames();
				while (e.hasMoreElements()) {
					final String attr = e.nextElement();
					LOG.debug("Session[%s]: %s", attr, httpSession.getAttribute(attr));
				}
				LOG.debug("=================================");
			}
		}

		// TODO register connection & notify context listener

	}

	HandshakeRequest getHandshakeRequest() {
		return (HandshakeRequest) mConfig.getUserProperties().get(DTALK_HANDSHAKE_REQUEST_KEY);
	}
	
	BrokerMessageHandler getMessageHandler() {
		return (BrokerMessageHandler) mConfig.getUserProperties().get(BROKER_MESSAGE_HANDLER_KEY);
	}

	@OnClose
	public void onClose() {
		LOG.trace(">>> onClose: {}", mSession.getId());

		// TODO remove connection form connection registry & notify context listener

	}

	@OnMessage
	public void onMessage(String message) {
		message = StringUtils.deleteWhitespace(message);

		if (LOG.isTraceEnabled()) {
			String _message = message;
			if (_message.length() > 64) {
				_message = _message.substring(0, 64) + "...";
			}
			LOG.trace(">>> onMessage: {}", _message);
		}

		// Handle message.
		getMessageHandler().onMessage(this, message);
	}

	@OnError
	public void onError(Throwable exception, Session session) {
		LOG.error(">>> onError: (session: {})", session.getId(), exception);
		close();
	}

	@Override
	public Future<Void> sendMessage(String message) {
		message = StringUtils.deleteWhitespace(message);

		if (LOG.isTraceEnabled()) {
			String _message = message;
			if (_message.length() > 64) {
				_message = _message.substring(0, 64) + "...";
			}
			LOG.trace(">>> onMessage: {}", _message);
		}

		return mSession.getAsyncRemote().sendText(message);
	}

	@Override
	public void close() {
		LOG.trace(">>> close");

		try {
			mSession.close();
		} catch (IOException e) {
			// TODO remove connection form connection registry & notify context
			// listener
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mSession == null) ? 0 : mSession.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DTalkServerEndpoint other = (DTalkServerEndpoint) obj;
		if (mSession == null) {
			if (other.mSession != null)
				return false;
		} else if (!mSession.equals(other.mSession))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DTalkServerEndpoint [mSession=" + mSession.getId() + "]";
	}

}