package freddo.dtalk.broker.netty;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import freddo.dtalk.DTalk;
import freddo.dtalk.broker.BrokerMessageHandler;
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

	private final BrokerMessageHandler mMessageHandler = new BrokerMessageHandler();

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

		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
			return;
		}

		// WebSocket Handshake
		if (req.getMethod() == HttpMethod.GET && req.getUri().startsWith(DTalk.DTALKSRV_PATH)) {
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true);
			WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
			if (handshaker == null) {
				WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				// TODO default authentication here
				handshaker.handshake(ctx.channel(), req);
				synchronized (mChannelMapper) {
					NettyChannel channel = new NettyChannel(ctx, handshaker);
					channel.setIdleTime(60);
					mChannelMapper.put(ctx, channel);
				}
			}
		} else {
			// TODO
		}
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
		LOG.trace(">>> handleWebSocketFrame: {}", frame);

		// Check for ping frame
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// Check for pong frame
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

		// Reject binary frames
		if (!(frame instanceof TextWebSocketFrame)) {
			LOG.warn("{} frame types not supported", frame.getClass().getName());
			return;
		}

		// Get message
		String message = ((TextWebSocketFrame) frame).text();
		message = StringUtils.deleteWhitespace(message);

		if (LOG.isDebugEnabled()) {
			String _message = message;
			if (_message.length() > 64) {
				_message = _message.substring(0, 64) + "...";
			}
			LOG.debug("message: {}", _message);
		}

		// ctx.channel().writeAndFlush(new
		// TextWebSocketFrame(message.toUpperCase()));

		// Handle message.
		mMessageHandler.onMessage(channel, message);
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
