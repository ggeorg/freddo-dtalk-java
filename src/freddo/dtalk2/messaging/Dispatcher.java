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
package freddo.dtalk2.messaging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.veracloud.jton.JtonElement;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;
import freddo.dtalk2.services.DTalkService;

public class Dispatcher extends DTalkService {
	private static final Logger LOG = LoggerFactory.getLogger(Dispatcher.class);

	private HandlerRegistration mInboundMessageHR = null;
	private HandlerRegistration mOutboundMessageHR = null;

	final Map<String, Subscriber> mSubscribers;

	public Dispatcher() {
		super("dtalk.Dispatcher");
		mSubscribers = new ConcurrentHashMap<String, Subscriber>();
	}

	public void start() {
		removeHandlers();
		addHandlers();
	}

	@Override
	protected void onShutdown() {
		removeHandlers();
		mSubscribers.clear();
	}

	// @DTalkAction(name = "subscribe")
	public void subscribe(DTalkMessage message) {
		LOG.trace(">>> subscribe: {}", message);
		JtonElement params = message.getParams();
		if (params != null) {
			if (params.isJtonPrimitive()) {
				String topic = params.getAsString(null);
				if (topic != null) {
					LOG.debug("Subscibe: topic='{}'", topic);
					String from = message.getFrom();
					if (from != null && DTalk.hasPresence(from)) {
						String regKey = String.format("%s@%s", topic, from);
						subscribe(regKey, topic, from);
					}

					// TODO return...
					fireEvent("onsubscribe", params);
				}
			}
		}
	}

	private void subscribe(String regKey, String topic, String target) {
		synchronized (mSubscribers) {
			if (!mSubscribers.containsKey(regKey)) {
				LOG.debug("New subscription: {}", regKey);
				mSubscribers.put(regKey, new Subscriber(this, topic, target));
			} else {
				LOG.debug("Increase refCnt: {}", regKey);
				mSubscribers.get(regKey).incRefCnt();
			}
		}
	}

	// @DTalkAction(name = "unsubscribe")
	public void unsubscribe(DTalkMessage message) {
		LOG.trace(">>> unsubscribe: {}", message);
		JtonElement params = message.getParams();
		if (params != null) {
			if (params.isJtonPrimitive()) {
				String topic = params.getAsString(null);
				if (topic != null) {
					String from = message.getFrom();
					if (from != null && DTalk.hasPresence(from)) {
						String regKey = String.format("%s@%s", topic, from);
						unsubscribe(regKey);
					}

					// TODO return...
					fireEvent("onunsubscribe", params);
				}
			}
		}
	}

	private void unsubscribe(String regKey) {
		synchronized (mSubscribers) {
			Subscriber subscriber = mSubscribers.get(regKey);
			if (subscriber != null) {
				if (subscriber.decRefCnt() == 0) {
					LOG.debug("Remove subscription: {}", regKey);
					subscriber.unsubscribe();
					mSubscribers.remove(regKey);
				} else {
					LOG.debug("Decrease refCnt: {}", regKey);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void addHandlers() {
		mInboundMessageHR = DTalk.subscribe(DTalk.DTALK_INBOUND_MSG, new InboundMsgHandler());
		mOutboundMessageHR = DTalk.subscribe(DTalk.DTALK_OUTBOUND_MSG, new OutboundMsgHandler());
	}

	private void removeHandlers() {
		if (mInboundMessageHR != null) {
			mInboundMessageHR.removeHandler();
			mInboundMessageHR = null;
		}
		if (mOutboundMessageHR != null) {
			mOutboundMessageHR.removeHandler();
			mOutboundMessageHR = null;
		}
	}

}
