package freddo.dtalk;

import com.arkasoft.jton.JtonObject;

public class DTalkMessage {

	public static final String KEY_TO = "to";
	public static final String KEY_FROM = "from";
	public static final String KEY_TOPIC = "service";
	public static final String KEY_ACTION = "action";

	private final JtonObject mMessage;

	protected DTalkMessage(JtonObject message) {
		mMessage = message;
	}

	public String getTo() {
		return mMessage.get(KEY_TO).getAsString();
	}

	public void setTo(String to) {

	}

	public String getFrom() {
		return mMessage.get(KEY_FROM).getAsString();
	}

	public void setFrom(String to) {

	}

	public String getTopic() {
		return mMessage.get(KEY_TOPIC).getAsString();
	}

	public void setTopic(String topic) {

	}

	public String getAction() {
		return mMessage.get(KEY_ACTION).getAsString();
	}

	public void setAction(String action) {

	}

}
