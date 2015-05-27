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
package freddo.messagebus;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides support for basic intra-application message passing.
 */
public class MessageBus {
	
	private final Map<String, ListenerList<MessageBusListener<?>>> messageTopics = 
			new HashMap<String, ListenerList<MessageBusListener<?>>>();

	/**
	 * Subscribes a listener to a message topic.
	 * 
	 * @param topic
	 * @param messageListener
	 */
	public <T> void subscribe(Class<? super T> topic, MessageBusListener<T> messageListener) {
		subscribe(topic.getName(), messageListener);
	}

	public <T> void subscribe(String topic, MessageBusListener<T> messageListener) {
		ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

		if (topicListeners == null) {
			topicListeners = new ListenerList<MessageBusListener<?>>() {
				// empty block
			};
			messageTopics.put(topic, topicListeners);
		}

		topicListeners.add(messageListener);
	}

	/**
	 * Unsubscribe a listener from a message topic.
	 * 
	 * @param topic
	 * @param messageListener
	 */
	public <T> void unsubscribe(Class<? super T> topic, MessageBusListener<T> messageListener) {
		unsubscribe(topic.getName(), messageListener);
	}

	public <T> void unsubscribe(String topic, MessageBusListener<T> messageListener) {
		ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

		if (topicListeners == null) {
			throw new IllegalArgumentException(topic + " does not exist.");
		}

		topicListeners.remove(messageListener);
		if (topicListeners.isEmpty()) {
			messageTopics.remove(topic);
		}
	}

	/**
	 * Sends a message to subscribed topic listeners.
	 * 
	 * @param message
	 */
	public <T> void sendMessage(T message) {
		sendMessage(message.getClass().getName(), message);
	}

	@SuppressWarnings("unchecked")
	public <T> void sendMessage(String topic, T message) {
		ListenerList<MessageBusListener<?>> topicListeners = messageTopics.get(topic);

		if (topicListeners != null) {
			for (MessageBusListener<?> listener : topicListeners) {
				((MessageBusListener<T>) listener).messageSent(message);
			}
		}
	}
}