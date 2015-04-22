package freddo.dtalk2.broker;

import freddo.dtalk2.DTalkConfiguration;

public interface Broker {
	
	void initialize(DTalkConfiguration config);

	void start();
	
	void shutdown();

}
