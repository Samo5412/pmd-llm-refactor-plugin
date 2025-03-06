package com.project.exception;

/**
 * Exception thrown to indicate a network failure.
 *
 * @author Sara Moussa.
 */
public class NetworkFailureException extends RuntimeException {

    /**
     * Constructs a new NetworkFailureException with the specified message and cause.
     *
     * @param message the message to be displayed.
     * @param cause the cause of the exception.
     */
    public NetworkFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}