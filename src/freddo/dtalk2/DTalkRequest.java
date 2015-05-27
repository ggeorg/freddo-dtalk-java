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
package freddo.dtalk2;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class DTalkRequest implements DTalkMessage.Handler, Runnable {

	private final DTalkMessage mRequest;
	private HandlerRegistration mHR = null;
	private ScheduledFuture<?> mScheduledFuture = null;

	public DTalkRequest(DTalkMessage request) {
		mRequest = request;
		String id = request.getId();
		if (id == null) {
			StringBuilder sb = new StringBuilder();
			sb.append('$')
					.append(request.getService())
					.append('#')
					.append(request.getAction())
					.append('-')
					.append(hashCode());
			id = sb.toString();
			request.setId(id);
		}
	}

	@Deprecated
	final void send(ScheduledExecutorService executor, long delay) {
		mScheduledFuture = executor.schedule(this, delay, TimeUnit.MICROSECONDS);
		mHR = DTalk.subscribe(this);
		DTalk.sendMessage(mRequest);
	}

	@Override
	public final void messageSent(DTalkMessage message) {
		if (mScheduledFuture == null || mScheduledFuture.isDone()) {
			// XXX
			return;
		} else {
			mScheduledFuture.cancel(true);
		}

		if (mHR != null) {
			mHR.removeHandler();
			mHR = null;
		}

		// TODO send response
	}

	protected abstract void onSuccess(DTalkMessage response);

	protected abstract void onFailure(Throwable cause);

	@Override
	public final String getTopic() {
		return mRequest.getId();
	}

	@Override
	public final void run() {
		if (mHR != null) {
			mHR.removeHandler();
			mHR = null;

			// TODO sent timeout error
		}
		mScheduledFuture = null;
	}

}
