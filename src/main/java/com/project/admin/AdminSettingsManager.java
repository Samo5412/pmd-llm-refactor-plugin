package com.project.admin;

import com.project.exception.MissingAPIKeyException;

import java.util.prefs.Preferences;

/**
 * Manages API key and URL settings for administrators.
 * TODO: Improve security for storing sensitive data
 * @author Sara Moussa
 */
public class AdminSettingsManager {

    /** User preferences storage */
    private final Preferences prefs;

    /** Preferences key for storing the Openrouter API key */
    private static final String API_KEY = "openrouter_api_key";

    /** Preferences key for storing the Openrouter API URL */
    private static final String API_URL = "openrouter_api_url";

    /**
     * Initializes the admin settings manager with preferences node.
     */
    public AdminSettingsManager() {
        this.prefs = Preferences.userRoot().node("com.project.admin");
    }

    /**
     * Retrieves the stored API key.
     *
     * @return API key if available.
     * @throws IllegalStateException if the API key is not set.
     */
    public String getApiKey() {
        String apiKey = prefs.get(API_KEY, null);
        if (apiKey == null || apiKey.isEmpty()) {
            throw new MissingAPIKeyException("API key is missing.");
        }
        return apiKey;
    }

    /**
     * Updates the API key.
     *
     * @param newApiKey The new API key.
     */
    public void setApiKey(String newApiKey) {
        if (newApiKey == null || newApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid API key. Cannot be empty.");
        }
        prefs.put(API_KEY, newApiKey);
    }

    /**
     * Retrieves the stored API URL, defaulting if missing.
     *
     * @return API URL.
     */
    public String getApiUrl() {
        return prefs.get(API_URL, "https://openrouter.ai/api/v1");
    }

    /**
     * Updates the API URL.
     *
     * @param newApiUrl The new API URL.
     */
    public void setApiUrl(String newApiUrl) {
        if (newApiUrl == null || !newApiUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid API URL. Must start with 'https://'.");
        }
        prefs.put(API_URL, newApiUrl);
    }

    /**
     * Resets API key and URL (For Testing Purposes).
     */
    public void resetSettings() {
        prefs.remove(API_KEY);
        prefs.remove(API_URL);
    }
}
