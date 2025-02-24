package com.project.api.exceptions;

/**
 * Exception thrown when the LLM API key is missing from the environment variables.
 *
 * @author Sara Moussa
 */
public class MissingAPIKeyException extends RuntimeException {
    /**
     * Constructs a new MissingAPIKeyException with the specified detail message.
     *
     * @param message the detail message
     */
    public MissingAPIKeyException(String message) {
        super(message);
    }
}
