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
package freddo.dtalk2.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalkConnection;
import freddo.dtalk2.client.ClientConnection;

public class NettyClient implements ClientConnection {
	private static final Logger LOG = LoggerFactory.getLogger(NettyClient.class);

	private final String mName;

	private Channel mChannel = null;

	public NettyClient(String name, URI uri) {
		mName = name;

		// connect...
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			String protocol = uri.getScheme();
			if (!"ws".equals(protocol)) {
				throw new IllegalArgumentException("Unsupported protocol: " + protocol);
			}

			HttpHeaders customHeaders = new DefaultHttpHeaders();
			// customHeaders.add("MyHeader", "MyValue");

			// Connect with V13 (RFC 6455 aka HyBi-17).
			final NettyClientHandler handler =
					new NettyClientHandler(this, WebSocketClientHandshakerFactory.newHandshaker(uri,
							WebSocketVersion.V13, null, false, customHeaders));

			b.group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast("http-codec", new HttpClientCodec());
							pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
							pipeline.addLast("ws-handler", handler);
						}
					});

			LOG.debug("WebSocket Client connecting...");
			mChannel = b.connect(uri.getHost(), uri.getPort()).addListener(new GenericFutureListener<ChannelFuture>() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if (!f.isSuccess()) {
						LOG.debug("Connection error", f.cause());
					}
				}
			}).sync().channel();

			LOG.debug("Handshake...");
			handler.handshakeFuture().sync();
		} catch (Exception e) {
			LOG.debug("Error: {}", e.getMessage());
			// e.printStackTrace();

			group.shutdownGracefully();
			group = null;
		}
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public void setName(String name) {
		throw new UnsupportedOperationException("Can't change connection name.");
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
