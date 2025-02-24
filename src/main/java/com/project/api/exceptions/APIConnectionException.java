package com.project.api.exceptions;

/**
 * Exception thrown when there is a failure in connecting to the API.
 *
 * @author Sara Moussa
 */
public class APIConnectionException extends Exception {
    /**
     * Constructs a new APIConnectionException with the specified message.
     *
     * @param message The error message.
     */
    public APIConnectionException(String message) {
        super(message);
    }
}
