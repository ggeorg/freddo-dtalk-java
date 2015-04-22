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
					.append(request.getTopic())
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
