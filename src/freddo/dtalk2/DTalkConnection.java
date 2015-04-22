package freddo.dtalk2;

import java.util.concurrent.Future;

/**
 * A construct that provides the means to send an ordered, lossless, stream of
 * bytes in both directions.
 * 
 * @author ggeorg
 */
public interface DTalkConnection {

	String getId();

	Future<Void> sendMessage(String message);

	void close();

}
