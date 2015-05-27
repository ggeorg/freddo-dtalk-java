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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkMessage;
import freddo.dtalk2.HandlerRegistration;

class Subscriber implements DTalkMessage.Handler {
	private final static Logger LOG = LoggerFactory.getLogger(Subscriber.class);

	private volatile int mRefCnt = 1;

	private final Dispatcher mDispatcher;
	private final String mTopic;
	private final String mTarget;

	private HandlerRegistration mHR = null;

	public Subscriber(Dispatcher dispatcher, String topic, String target) {
		mDispatcher = dispatcher;
		mTopic = topic;
		mTarget = target;
		mHR = DTalk.subscribe(this);
	}

	@Override
	public String getTopic() {
		return mTopic;
	}

	int incRefCnt() {
		return ++mRefCnt;
	}

	int decRefCnt() {
		return --mRefCnt;
	}

	@Override
	public void messageSent(DTalkMessage message) {
		LOG.trace(">>> messageSend: {}", message);

		// Get connection by name.
		if (DTalk.getConnection(mTarget) == null) {
			LOG.debug("Connection not found: '{}'!", mTarget);
			// Check presence by name.
			if (DTalk.getPresence(mTarget) == null) {
				LOG.debug("Connection not found: '{}'!", mTarget);
				unsubscribe();
				// ... also remove from Dispatcher.
				String regKey = String.format("%s@%s", mTopic, mTarget);
				mDispatcher.mSubscribers.remove(regKey);
			}
		}

		// Clone message.
		DTalkMessage _message = new DTalkMessage(message);
		
		// Set message 'to'.
		_message.setTo(mTarget);

		// Queue message.
		DTalk.sendMessage(_message);
	}

	public void unsubscribe() {
		LOG.trace(">>> unsubscribe");
		if (mHR != null) {
			mHR.removeHandler();
			mHR = null;
		}
	}

}
