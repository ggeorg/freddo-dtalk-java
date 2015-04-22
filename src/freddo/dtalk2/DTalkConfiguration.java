package freddo.dtalk2;

import java.net.InetSocketAddress;

import freddo.dtalk2.broker.Broker;

public interface DTalkConfiguration {

	Class<? extends Broker> getBrokerClass();
	
	InetSocketAddress getSocketAddress();

}
