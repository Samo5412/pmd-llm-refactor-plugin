package com.project.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for logging messages across the application.
 * Provides logging methods that dynamically identify the calling class.
 */
public class LoggerUtil {

    /**
     * Retrieves a logger for the calling class.
     * @return Logger instance for the caller
     */
    private static Logger getLogger() {
        String className = new Throwable().getStackTrace()[2].getClassName();
        return LogManager.getLogger(className);
    }

    /**
     * Logs an informational message.
     * @param message the message to log
     */
    public static void info(String message) {
        getLogger().info(message);
    }

    /**
     * Logs a warning message.
     * @param message the message to log
     */
    public static void warn(String message) {
        getLogger().warn(message);
    }

    /**
     * Logs a debug message.
     * @param message the message to log
     */
    public static void debug(String message) {
        getLogger().debug(message);
    }

    /**
     * Logs an error message.
     * @param message the message to log
     * @param throwable the exception to log (can be null)
     */
    public static void error(String message, Throwable throwable) {
        if (throwable != null) {
            getLogger().error(message, throwable);
        } else {
            getLogger().error(message);
        }
    }
}
