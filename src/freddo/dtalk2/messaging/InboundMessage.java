package freddo.dtalk2.messaging;

import com.arkasoft.jton.JtonObject;

import freddo.dtalk2.DTalkConnection;
import freddo.dtalk2.DTalkMessage;

public class InboundMessage extends DTalkMessage {
	private final DTalkConnection mConnection;

	public InboundMessage(DTalkConnection conn, JtonObject message) {
		super(message);
		mConnection = conn;
	}

	@Deprecated
	public DTalkConnection getConnection() {
		return mConnection;
	}

	@Override
	public String toString() {
		return "InboundMessage [mConnection=" + mConnection + "]";
	}

}
