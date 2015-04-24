package freddo.dtalk2.discovery;

import freddo.dtalk2.DTalkConfiguration;
import freddo.dtalk2.services.DTalkService;

public abstract class MDNS extends DTalkService {

	public MDNS() {
		super("dtalk.service.MDNS");
	}
	
	public abstract void initialize(DTalkConfiguration config);

	public abstract void start();

}
