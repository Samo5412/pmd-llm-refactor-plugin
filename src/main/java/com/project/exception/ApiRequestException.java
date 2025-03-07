package com.project.exception;

/**
 * Exception thrown when an API request fails due to connectivity issues.
 *
 * @author Sara Moussa
 */
public class ApiRequestException extends RuntimeException {

    /**
     * Constructs a new ApiRequestException with the specified detail message and cause.
     *
     * @param message the message to be displayed.
     * @param cause the cause of the exception.
     */
    public ApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
