package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
    public static String REQUESTED_API_VERSION = "1.6";
    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();
    public static final StaticLoggerBinder getSingleton() {
        return INSTANCE;
    }

    private static final String factoryClassName = SimpleLoggerFactory.class.getName();
    private final ILoggerFactory loggerFactory;

    private StaticLoggerBinder() {
        loggerFactory = new SimpleLoggerFactory();
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
      return factoryClassName;
    }
}
