package com.project.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the storage and retrieval of the last prompt and code snippets.
 */
public class RequestStorage {

    /**
     * The last prompt that was sent to the LLM API.
     */
    private static String lastPrompt;

    /**
     * The list of code snippets.
     */
    private static final List<String> codeSnippets = new ArrayList<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private RequestStorage() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the last prompt that was sent to the LLM API.
     * @return The last prompt.
     */
    public static String getLastPrompt() {
        return lastPrompt;
    }

    /**
     * Sets the last prompt that was sent to the LLM API.
     * @param lastPrompt The last prompt.
     */
    public static void setLastPrompt(String lastPrompt) {
        RequestStorage.lastPrompt = lastPrompt;
    }

    /**
     * Adds a code snippet to the storage.
     * @param codeSnippet The code snippet to add.
     */
    public static void addCodeSnippet(String codeSnippet) {
        codeSnippets.add(codeSnippet);
    }

    /**
     * Returns the list of code snippets.
     * @return The list of code snippets.
     */
    public static List<String> getCodeSnippets() {
        return new ArrayList<>(codeSnippets);
    }

    /**
     * Clears all stored code snippets.
     */
    public static void clearCodeSnippets() {
        codeSnippets.clear();
    }
}