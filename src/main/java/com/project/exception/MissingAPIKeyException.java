package com.project.exception;

/**
 * Throws an exception when the API key is missing in the environment variables.
 */
public class MissingAPIKeyException extends RuntimeException {

    /**
     * Constructs a new MissingAPIKeyException with the specified message.
     *
     * @param message the message to be displayed.
     */
    public MissingAPIKeyException(String message) {
        super(message);
    }
}
