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
package freddo.dtalk2.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.veracloud.jton.JtonArray;
import com.veracloud.jton.JtonElement;
import com.veracloud.jton.serialization.JsonSerializer;

import freddo.dtalk2.DTalk;
import freddo.dtalk2.DTalkConnection;
import freddo.dtalk2.DTalkMessage;

public final class BrokerMessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(BrokerMessageHandler.class);

	public static void onMessage(DTalkConnection conn, String message) {
		try {
			JtonElement jtonElem = JsonSerializer.parse(message);
			if (jtonElem.isJtonObject()) {
				onMessage(new DTalkMessage(conn, jtonElem.getAsJtonObject()));
			} else if (jtonElem.isJtonArray()) {
				onMessage(conn, jtonElem.getAsJtonArray());
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	private static void onMessage(DTalkConnection conn, JtonArray jtonArray) {
		for (JtonElement jtonElem_i : jtonArray.getAsJtonArray()) {
			if (jtonElem_i.isJtonObject()) {
				onMessage(new DTalkMessage(conn, jtonElem_i.getAsJtonObject()));
			}
		}
	}

	private static void onMessage(DTalkMessage message) {
		LOG.trace(">>> onMessage: {}", message);
		
		try {
			String from = message.getFrom();
			if (from == null) {
				message.setFrom(message.getConnection().getName());
			}
			
			int version = message.getVersion();
			if (version == 1 || version == 2) {
				String service = message.getService();
				if (service == null || service.trim().length() == 0) {
					String action = message.getAction();
					if ("connect".equals(action)) {
						LOG.debug("Handle CONNECT");
						if (from != null) {
							message.getConnection().setName(from);
						}
						DTalk.addConnection(message.getConnection());
						// TODO reply with OK or ERROR
					} else {
						// TODO if not authenticated reply with ERROR
						if ("disconnect".equals(action)) {
							LOG.debug("Handle DISCONNECT");
							// TODO ???
						}
					}
				} else {
					DTalk.sendMessage(message);
				}
			} else {
				LOG.warn("DTalk version '{}' not supported.", version);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Default constructor.
	 */
	private BrokerMessageHandler() {
		// hidden
	}

}
