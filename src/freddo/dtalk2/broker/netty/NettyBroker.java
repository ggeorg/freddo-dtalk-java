package freddo.dtalk2.broker.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalkConfiguration;
import freddo.dtalk2.broker.Broker;

public class NettyBroker implements Broker {
	private static final Logger LOG = LoggerFactory.getLogger(NettyBroker.class);

	private static final String sDefaultHost = "localhost";
	private static final int sDefaultPort = 0;

	private NioEventLoopGroup mBossGroup;
	private NioEventLoopGroup mWorkerGroup;

	private InetSocketAddress mSocketAddress;

	public InetSocketAddress getSocketAddress() {
		return mSocketAddress;
	}

	@Override
	public void initialize(DTalkConfiguration config) {
		mBossGroup = new NioEventLoopGroup();
		mWorkerGroup = new NioEventLoopGroup();
		mSocketAddress = config.getSocketAddress();
		if (mSocketAddress == null) {
			mSocketAddress = new InetSocketAddress(sDefaultHost, sDefaultPort);
		}
	}

	@Override
	public void start() {
		ServerBootstrap b = new ServerBootstrap();
		b.group(mBossGroup, mWorkerGroup)
				.handler(new LoggingHandler(LogLevel.INFO))
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						// if (sslCtx != null) {
						// pipeline.addLast(sslCtx.newHandler(ch.alloc()));
						// }
						pipeline.addLast(new HttpServerCodec());
						pipeline.addLast(new HttpObjectAggregator(65536));
						pipeline.addLast(new NettyBrokerHandler());
					}
				});

		try {
			// Bind and start to accept incoming connections.
			Channel ch = b.bind(mSocketAddress).sync().channel();
			mSocketAddress = (InetSocketAddress) ch.localAddress();
			LOG.info("Server binded host: {}, port: {}", mSocketAddress.getHostName(), mSocketAddress.getPort());
		} catch (InterruptedException ex) {
			LOG.error(null, ex);
		}
	}

	@Override
	public void shutdown() {
		LOG.trace(">>> shutdown");
		
		if (mWorkerGroup == null) {
			throw new IllegalStateException("Invoked close on a Broker that wasn't initialized");
		}
		if (mBossGroup == null) {
			throw new IllegalStateException("Invoked close on a Broker that wasn't initialized");
		}
		
		mWorkerGroup.shutdownGracefully();
		mBossGroup.shutdownGracefully();
	}

}
