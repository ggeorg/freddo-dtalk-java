package freddo.dtalk;

import javax.ejb.Stateless;

import freddo.dtalk.util.MessageBus;

@Stateless
public class DTalk {

	private final MessageBus mMessageBus;
	private final String mIdentifier;

	public DTalk() {
		this("default");
	}

	public DTalk(String identifier) {
		mMessageBus = new MessageBus();
		mIdentifier = identifier;
	}

	public String getmIdentifier() {
		return mIdentifier;
	}

	// initialize broker & start server...
	
	public void post(String topic, DTalkMessage message) {
		mMessageBus.sendMessage(topic, message);
	}

}
