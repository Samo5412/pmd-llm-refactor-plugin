package com.project.ui.settings;

/**
 * Represents the result of a validation operation, encapsulating its validity, error message, and validated value.
 *
 * @author Sara Moussa
 */
public class ValidationSettings {
    /**
     * Indicates whether the validation was successful. True if the validation passed, false otherwise.
     */
    private final boolean isValid;
    /**
     * Describes the error or reason for validation failure. Null if validation is successful.
     */
    private final String errorMessage;
    /**
     * Holds the value successfully processed during validation or null if validation failed.
     */
    private final Object validatedValue;

    /**
     * Creates a new instance of ValidationSettings with specified validation status, error message, and validated value.
     *
     * @param isValid indicates whether the validation was successful.
     * @param errorMessage the error message associated with the validation if it failed; null if successful.
     * @param validatedValue the value that was validated; null if validation failed.
     */
    private ValidationSettings(boolean isValid, String errorMessage, Object validatedValue) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.validatedValue = validatedValue;
    }

    /**
     * Creates a successful validation result with the provided value.
     *
     * @param value the object being validated
     * @return a ValidationSettings instance indicating a valid result, with the given value
     */
    public static ValidationSettings valid(Object value) {
        return new ValidationSettings(true, null, value);
    }

    /**
     * Creates an instance representing an invalid validation result with the given error message.
     *
     * @param errorMessage the message describing the validation error
     * @return a ValidationSettings instance marked as invalid, containing the specified error message
     */
    public static ValidationSettings invalid(String errorMessage) {
        return new ValidationSettings(false, errorMessage, null);
    }

    /**
     * Indicates the validity status, providing the opposite of the internal validation flag.
     *
     * @return true if the internal state is invalid, false otherwise.
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Retrieves the error message associated with an invalid validation result.
     *
     * @return the error message if validation is invalid; null if validation is valid.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Retrieves the validated value cast to the specified type.
     *
     * @return the validated value, or null if validation failed or no value exists
     * @throws ClassCastException if the value cannot be cast to the desired type
     */
    public <T> T getValue() {
        return (T) validatedValue;
    }
}
