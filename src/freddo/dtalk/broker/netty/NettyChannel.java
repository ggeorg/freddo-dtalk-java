package freddo.dtalk.broker.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.Future;

import freddo.dtalk.broker.Connection;

public class NettyChannel implements Connection {

	public static final String ATTR_CLIENTID = "ClientID";
	public static final String CLEAN_SESSION = "cleanSession";
	public static final String KEEP_ALIVE = "keepAlive";

	public static final AttributeKey<Object> ATTR_KEY_KEEPALIVE = AttributeKey.valueOf(KEEP_ALIVE);
	public static final AttributeKey<Object> ATTR_KEY_CLEANSESSION = AttributeKey.valueOf(CLEAN_SESSION);
	public static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey.valueOf(ATTR_CLIENTID);

	private final ChannelHandlerContext mChannel;
	private final WebSocketServerHandshaker mHandshaker;

	NettyChannel(ChannelHandlerContext ctx, WebSocketServerHandshaker handshaker) {
		mChannel = ctx;
		mHandshaker = handshaker;
	}

	ChannelHandlerContext getChannel() {
		return mChannel;
	}

	WebSocketServerHandshaker getHandshaker() {
		return mHandshaker;
	}

	public Object getAttribute(AttributeKey<Object> key) {
		Attribute<Object> attr = mChannel.attr(key);
		return attr.get();
	}

	public void setAttribute(AttributeKey<Object> key, Object value) {
		Attribute<Object> attr = mChannel.attr(key);
		attr.set(value);
	}

	@Override
	public Future<Void> sendMessage(String message) {
		return mChannel.write(message);
	}

	@Override
	public void close() {
		mChannel.close();
	}

}
