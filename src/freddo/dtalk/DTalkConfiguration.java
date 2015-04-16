package freddo.dtalk;

import java.net.InetSocketAddress;

import freddo.dtalk.broker.Broker;

public interface DTalkConfiguration {

	Class<? extends Broker> getBrokerClass();
	
	InetSocketAddress getSocketAddress();

}
