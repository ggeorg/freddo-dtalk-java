package freddo.dtalk2.messaging;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk2.DTalkMessage;

public class OutboundMessage extends DTalkMessage {
	
	public OutboundMessage(JtonObject message) {
		super(message);
	}

	OutboundMessage(DTalkMessage message) {
		super(message);
	}

}