package freddo.dtalk.broker;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk.DTalkMessage;

public class OutgoingMessage extends DTalkMessage {
	public OutgoingMessage(JtonObject message) {
		super(message);
	}
}