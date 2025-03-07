package com.project.exception;

/**
 * Exception thrown when an API response is invalid or unexpected.
 *
 * @author Sara Moussa
 */
public class ApiResponseException extends RuntimeException {

    /**
     * Constructs a new ApiResponseException with the specified message.
     *
     * @param message the message to be displayed.
     */
    public ApiResponseException(String message) {
        super(message);
    }
}
