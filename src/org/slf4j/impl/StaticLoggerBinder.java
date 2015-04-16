package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  public static String REQUESTED_API_VERSION = "1.6";

  public static final StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }

  private StaticLoggerBinder() {
  }

	@Override
	public ILoggerFactory getLoggerFactory() {
		return new Log4jLoggerFactory();
	}

	@Override
	public String getLoggerFactoryClassStr() {
		return Log4jLoggerFactory.class.getName();
	}

}
