/*
 * Copyright (c) 2013-2015 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk2.services;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.veracloud.jton.JtonElement;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;

public abstract class DTalkService implements DTalkMessage.Handler {
	private static final Logger LOG = LoggerFactory.getLogger(DTalkService.class);

	private final String mTopic;
	private final String mReplyPrefix;
	private HandlerRegistration mHR = null;

	/** Methods mapped by action (quick lookup table). */
	private final Map<String, Method> mActionHandlers = new ConcurrentHashMap<String, Method>();

	public DTalkService(String topic) {
		mTopic = topic;
		mReplyPrefix = String.format("$%s#", topic);
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
					if (response != null && message.getId() != null) {
						sendResponse(message, (JtonElement) response);
					}
				} catch (Exception e) {
					LOG.error("Method invocation failed: {}", method);
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
	
	// ---
	
	protected void sendResponse(DTalkMessage request, JtonElement response) {
		LOG.trace(">>> sendResponse: {}", response);
		DTalkMessage _response = new DTalkMessage();
		_response.setVersion(DTalk.VERSION);
		_response.setService(request.getId());
		_response.setResult(response);
		String from = request.getFrom();
		if (from != null) {
			_response.setTo(from);
		}
		DTalk.sendMessage(_response);
	}
	
	protected void fireEvent(String type, JtonElement params) {
		LOG.trace(">>> fireEvent: {}", type);
		StringBuilder sb = new StringBuilder();
		sb.append(mReplyPrefix).append(type);
		DTalkMessage _response = new DTalkMessage();
		_response.setVersion(DTalk.VERSION);
		_response.setService(sb.toString());
		_response.setParams(params);
		DTalk.sendMessage(_response);
	}

}
