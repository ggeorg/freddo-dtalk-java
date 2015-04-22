package freddo.dtalk2.services;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkasoft.jton.JtonElement;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;

public abstract class DTalkService implements DTalkMessage.Handler {
	private static final Logger LOG = LoggerFactory.getLogger(DTalkService.class);

	private final String mTopic;
	private HandlerRegistration mHR = null;

	/** Methods mapped by action (quick lookup table). */
	private final Map<String, Method> mActionHandlers = new ConcurrentHashMap<String, Method>();

	public DTalkService(String topic) {
		mTopic = topic;
		mHR = DTalk.subscribe(this);
	}

	@Override
	public String getTopic() {
		return mTopic;
	}

	@Override
	public final void messageSent(DTalkMessage message) {
		LOG.trace(">>> messageSent: {}", message);
		String action = message.getAction();
		if (action != null) {
			Method method = getActionHandler(action);
			if (method != null) {
				try {
					Object response = method.invoke(this, message);
					if (message.getId() != null) {
						DTalk.sendResponse(message, (JtonElement) response);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				// TODO No Method
			}
		} else {
			// TODO No Action
		}
	}

	private Method getActionHandler(String action) {
		Method method = null;
		if (action != null) {
			method = mActionHandlers.get(action);
			if (method == null) {
				Method[] methods = getClass().getMethods();
				for (Method m : methods) {
					DTalkAction annotation = m.getAnnotation(DTalkAction.class);
					if (annotation != null && action.equals(annotation.name())) {
						synchronized (mActionHandlers) {
							mActionHandlers.put(action, method = m);
						}
						break;
					}
				}
			}
		}
		return method;
	}

	public final void shutdown() {
		LOG.trace(">>> shutdown");

		try {
			onShutdown();
		} finally {
			if (mHR != null) {
				mHR.removeHandler();
				mHR = null;
			}
		}
	}

	protected abstract void onShutdown();

}
