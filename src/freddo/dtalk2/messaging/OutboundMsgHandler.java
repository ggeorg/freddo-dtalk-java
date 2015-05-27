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
import freddo.dtalk2.DTalkConnection;
import freddo.dtalk2.DTalkMessage;
import freddo.messagebus.MessageBusListener;

class OutboundMsgHandler implements MessageBusListener<DTalkMessage> {
	private static final Logger LOG = LoggerFactory.getLogger(OutboundMsgHandler.class);

	@Override
	public void messageSent(DTalkMessage message) {
		LOG.trace(">>> messageSent: {}", message);
		
		String to = message.getTo();
		if (to == null) {
			LOG.debug("Missing 'to', message='{}'", message);
			return;
		}
		
		DTalkMessage clonedMsg = new DTalkMessage(message);
		String from = message.getFrom();
		if (from == null) {
			clonedMsg.setFrom(DTalk.getInstance().getPublishedName());
		}
		
		// Get connection.
		DTalkConnection conn = DTalk.getConnection(to);
		
		// Create a new client connection if null.
		if (conn == null) {
			DTalk.getPresence(to);
		}
		
		if (conn != null) {
			//message.setFrom(DTalk.getLocal());
			conn.sendMessage(clonedMsg.toString());
		}
		
	}

}
