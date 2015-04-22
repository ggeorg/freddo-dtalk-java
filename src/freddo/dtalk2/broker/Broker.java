package freddo.dtalk.broker;

import freddo.dtalk.DTalkConfiguration;

public interface Broker {
	
	void initialize(DTalkConfiguration config);

	void start();
	
	void shutdown();

}
