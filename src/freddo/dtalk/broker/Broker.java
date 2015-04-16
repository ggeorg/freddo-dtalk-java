package freddo.dtalk.broker;

import java.util.Properties;

public interface Broker {
	
	void initialize(Properties properties);
	
	void shutdown();

}
