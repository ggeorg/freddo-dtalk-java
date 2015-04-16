package freddo.dtalk.broker;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk.DTalkMessage;

public class IncomingMessage extends DTalkMessage {
	public IncomingMessage(JtonObject message) {
		super(message);
	}
}
