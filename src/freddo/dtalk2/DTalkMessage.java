package freddo.dtalk2;

import com.arkasoft.jton.JtonElement;
import com.arkasoft.jton.JtonObject;

import freddo.messagebus.MessageBusListener;

public class DTalkMessage {

	public static interface Handler extends MessageBusListener<DTalkMessage> {
		String getTopic();
	}
	
	public static class Error {
		
		private final JtonObject mError;
		
		Error(JtonObject error) {
			mError = error;
		}
		
		public int getCode() {
			return mError.get(KEY_ERROR_CODE).getAsInt(0);
		}

		public String getMessage() {
			return mError.get(KEY_ERROR_MESSAGE).getAsString(null);
		}
		
		public JtonElement getData() {
			return mError.get(KEY_ERROR_DATA);
		}
	}

	public static final String KEY_ACTION = "action";
	public static final String KEY_ERROR = "error";
	public static final String KEY_ERROR_CODE = "code";
	public static final String KEY_ERROR_MESSAGE = "message";
	public static final String KEY_ERROR_DATA = "data";
	public static final String KEY_FROM = "from";
	public static final String KEY_ID = "id";
	public static final String KEY_PARAMS = "params";
	public static final String KEY_RESULT = "result";
	public static final String KEY_TO = "to";
	public static final String KEY_TOPIC = "topic";
	public static final String KEY_VERSION = "dtalk";

	private final JtonObject mMessage;

	public DTalkMessage() {
		mMessage = new JtonObject();
	}

	protected DTalkMessage(JtonObject message) {
		mMessage = message;
	}

	protected DTalkMessage(DTalkMessage message) {
		mMessage = message.mMessage;
	}

	public int getVersion() {
		return mMessage.get(KEY_VERSION).getAsInt(1);
	}

	public void setVersion(int version) {
		mMessage.add(KEY_VERSION, version);
	}

	public String getTo() {
		return mMessage.get(KEY_TO).getAsString(null);
	}

	public void setTo(String to) {
		mMessage.add(KEY_TO, to);
	}

	public String getFrom() {
		return mMessage.get(KEY_FROM).getAsString(null);
	}

	public void setFrom(String from) {
		mMessage.add(KEY_FROM, from);
	}

	public String getId() {
		return mMessage.get(KEY_ID).getAsString(null);
	}

	public void setId(String id) {
		mMessage.add(KEY_ID, id);
	}

	public String getTopic() {
		return mMessage.get(KEY_TOPIC).getAsString(null);
	}

	public void setTopic(String topic) {
		mMessage.add(KEY_TOPIC, topic);
	}

	public String getAction() {
		return mMessage.get(KEY_ACTION).getAsString(null);
	}

	public void setAction(String action) {
		mMessage.add(KEY_ACTION, action);
	}

	public JtonElement getParams() {
		return mMessage.get(KEY_PARAMS);
	}

	public void setParams(JtonElement params) {
		mMessage.add(KEY_PARAMS, params);
	}

	public JtonElement getResult() {
		return mMessage.get(KEY_RESULT);
	}

	public void setResult(JtonElement result) {
		mMessage.add(KEY_RESULT, result);
	}
	
	public Error getError() {
		JtonObject error = mMessage.get(KEY_ERROR).getAsJtonObject(null);
		return error != null ? new Error(error) : null;
	}

	public void setError(int code, String message, JtonElement data) {
		JtonObject error = new JtonObject();
		error.add(KEY_ERROR_CODE, code);
		error.add(KEY_ERROR_MESSAGE, message);
		error.add(KEY_ERROR_DATA, data);
		mMessage.add(KEY_ERROR, error);
	}

	@Override
	public String toString() {
		return mMessage.toString();
	}

}
