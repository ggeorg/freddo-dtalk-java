package freddo.dtalk2;

import java.net.InetSocketAddress;

import freddo.dtalk2.broker.Broker;
import freddo.dtalk2.discovery.MDNS;

public interface DTalkConfiguration {

	Class<? extends Broker> getBrokerClass();

	Class<? extends MDNS> getMDNSClass();
	
	InetSocketAddress getSocketAddress();

	String getServiceName();

}
