package freddo.dtalk2.util;

public abstract class StringUtils {
	
	public static boolean isNullOrEmpty(String s) {
		return null != s ? 0 == s.trim().length() : true;
	}
	
}