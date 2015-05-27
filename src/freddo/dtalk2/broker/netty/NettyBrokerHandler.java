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

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import freddo.dtalk2.DTalk;
import freddo.dtalk2.broker.BrokerMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyBrokerHandler extends SimpleChannelInboundHandler<Object> {
	private static final Logger LOG = LoggerFactory.getLogger(NettyBrokerHandler.class);

	private final Map<ChannelHandlerContext, NettyChannel> mChannelMapper =
			new HashMap<ChannelHandlerContext, NettyChannel>();

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object message) {
		LOG.trace(">>> channelRead");
		if (message instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) message);
		} else if (message instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) message);
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		LOG.trace(">>> channelInactive: {}", ctx);
		super.channelInactive(ctx);
		synchronized (mChannelMapper) {
			mChannelMapper.remove(ctx);
		}

		// TODO fire on close....
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOG.error(">>> exceptionCaught: {}", cause.getMessage());
		ctx.close();
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		LOG.trace(">>> handleHttpRequest: {}", req.getUri());

		if (!req.getDecoderResult().isSuccess()) {

			// WebSocket Handshake
			if (req.getMethod() == HttpMethod.GET && req.getUri().startsWith(DTalk.DTALKSRV_PATH)) {
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true);
				WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					handshaker.handshake(ctx.channel(), req);
					synchronized (mChannelMapper) {
						NettyChannel channel = new NettyChannel(ctx, handshaker);
						channel.setIdleTime(60);
						mChannelMapper.put(ctx, channel);
					}
				}
			}
		}

		sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		LOG.trace(">>> handleWebSocketFrame: {}", frame);

		// Check for PING frame
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// Check for PONG frame
		if (frame instanceof PongWebSocketFrame) {
			return;
		}

		// Lookup channel by ctx
		NettyChannel channel = mChannelMapper.get(ctx);
		if (channel == null) {
			// XXX
			return;
		}

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			channel.getHandshaker().close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}

		// Reject anything except text frames
		if (!(frame instanceof TextWebSocketFrame)) {
			LOG.warn("{} frame types not supported", frame.getClass().getName());
			return;
		}

		// Get message
		String message = ((TextWebSocketFrame) frame).text();
		message = StringUtils.deleteWhitespace(message);
		LOG.debug("message: {}", message);

		// Handle message.
		BrokerMessageHandler.onMessage(channel, message);
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			HttpHeaders.setContentLength(res, res.content().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		String location = req.headers().get(HOST) + DTalk.DTALKSRV_PATH;
		// if (NettyBroker.SSL) {
		// return "wss://" + location;
		// } else {
		return "ws://" + location;
		// }
	}

}
