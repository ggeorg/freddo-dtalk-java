/*
 * Copyright (c) 2013-2015 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk2.broker.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalkConnection;

public class NettyChannel implements DTalkConnection {
	private static final Logger LOG = LoggerFactory.getLogger(NettyChannel.class);

	public static final String ATTR_CLIENTID = "ClientID";
	public static final String CLEAN_SESSION = "cleanSession";
	public static final String KEEP_ALIVE = "keepAlive";

	public static final AttributeKey<Object> ATTR_KEY_KEEPALIVE = AttributeKey.valueOf(KEEP_ALIVE);
	public static final AttributeKey<Object> ATTR_KEY_CLEANSESSION = AttributeKey.valueOf(CLEAN_SESSION);
	public static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey.valueOf(ATTR_CLIENTID);

	private final ChannelHandlerContext mChannel;
	private final WebSocketServerHandshaker mHandshaker;
	
	private String mName;

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
	
	@Override
	public String getName() {
		return mName != null ? mName : (String) getAttribute(ATTR_KEY_CLIENTID);
	}
	
	@Override
	public void setName(String id) {
		mName = id;
	}

	public Object getAttribute(AttributeKey<Object> key) {
		Attribute<Object> attr = mChannel.attr(key);
		return attr.get();
	}

	public void setAttribute(AttributeKey<Object> key, Object value) {
		Attribute<Object> attr = mChannel.attr(key);
		attr.set(value);
	}

	public void setIdleTime(int idleTime) {
		if (mChannel.pipeline().names().contains("idleStateHandler")) {
			mChannel.pipeline().remove("idleStateHandler");
		}
		if (mChannel.pipeline().names().contains("idleEventHandler")) {
			mChannel.pipeline().remove("idleEventHandler");
		}
		mChannel.pipeline().addFirst("idleStateHandler", new IdleStateHandler(idleTime, idleTime / 2, 0));
		mChannel.pipeline().addAfter("idleStateHandler", "idleEventHandler", new ChannelDuplexHandler() {
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				LOG.trace(">>> userEventTriggered: {}", evt);
				if (evt instanceof IdleStateEvent) {
					IdleStateEvent e = (IdleStateEvent) evt;
					if (e.state() == IdleState.READER_IDLE) {
						LOG.debug("read idle");
						ctx.close();
					} else if (e.state() == IdleState.WRITER_IDLE) {
						LOG.debug("write idle");
						ctx.channel().writeAndFlush(new PingWebSocketFrame(Unpooled
                .copiedBuffer(new byte[] { 1, 2, 3, 4, 5, 6 })));
					}
				}
			}
		});
	}

	@Override
	public Future<Void> sendMessage(String message) {
		return mChannel.write(message);
	}

	@Override
	public void close() {
		mChannel.close();
	}

	@Override
	public String toString() {
		String clientID = (String) getAttribute(ATTR_KEY_CLIENTID);
		return "session [clientID: " + clientID + "]" + super.toString();
	}

}
