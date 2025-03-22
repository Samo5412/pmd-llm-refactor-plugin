package com.project.api;

/**
 * Handles the storage and retrieval of the last prompt.
 * TODO: Revisit this class design when LLM more integrated
 * @author Micael Olsson
 */
public class PromptStorage {

    /**
     * The last prompt that was sent to the LLM API.
     */
    private static String lastPrompt;

    /**
     * Private constructor to prevent instantiation.
     * @return last prompt.
     */
    public static String getLastPrompt() {
        return lastPrompt;
    }

    /**
     * Sets the last prompt that was sent to the LLM API.
     * @param lastPrompt the last prompt.
     */
    static void setLastPrompt(String lastPrompt) {
        PromptStorage.lastPrompt = lastPrompt;
    }
}