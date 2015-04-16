package freddo.dtalk.broker.netty;

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk.Constants;
import freddo.dtalk.broker.Broker;

public class NettyBroker implements Broker {
	private static final Logger LOG = LoggerFactory.getLogger(NettyBroker.class);
	
	private NioEventLoopGroup mBossGroup;
	private NioEventLoopGroup mWorkerGroup;

	@Override
	public void initialize(Properties props) {
		mBossGroup = new NioEventLoopGroup();
		mWorkerGroup = new NioEventLoopGroup();
		
		// Initialize WebSocket transfer.
		String webSocketPort = props.getProperty(Constants.WEB_SOCKET_PORT_PROPERTY_NAME);
		if (webSocketPort == null) {
			// TODO use default
		}
		int port = Integer.parseInt(webSocketPort);
		
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
