package com.project.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.PathManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the settings for the application, including API key, API URL, and PMD ruleset.
 */
public class SettingsManager {

    private static final String CREDENTIAL_SERVICE_NAME = "com.project.admin";
    private static final String API_KEY = "api_key";
    private static final String API_URL_FILE_NAME = "api_url.txt";
    private static final String RULESET_FILE_NAME = "pmd_ruleset.xml";
    private static SettingsManager instance;

    /**
     * Returns the singleton instance of the SettingsManager.
     * @return the singleton instance.
     */
    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    /**
     * Retrieves the stored API key.
     * @return the API key.
     */
    public String getApiKey() {
        CredentialAttributes attributes = new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return (credentials == null || credentials.getPasswordAsString().isEmpty()) ? "" : credentials.getPasswordAsString();
    }

    /**
     * Stores the given API key.
     * @param newApiKey the API key to store.
     */
    public void setApiKey(String newApiKey) {
        CredentialAttributes attributes = new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY);
        if ((newApiKey == null || newApiKey.trim().isEmpty())) {
            PasswordSafe.getInstance().set(attributes, null);
        } else {
            Credentials credentials = new Credentials(API_KEY, newApiKey);
            PasswordSafe.getInstance().set(attributes, credentials);
        }
    }

    /**
     * Retrieves the stored API URL.
     * @return the API URL.
     */
    public String getApiUrl() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", API_URL_FILE_NAME);
        try {
            return Files.readString(configPath).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Error reading API URL file.", e);
        }
    }

    /**
     * Stores the given API URL.
     * @param newApiUrl the API URL to store.
     */
    public void setApiUrl(String newApiUrl) {
        if (newApiUrl == null || newApiUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid API URL. Cannot be empty.");
        } else if (!newApiUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid API URL. Must start with 'https://'");
        }
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", API_URL_FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, newApiUrl);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing API URL file.", e);
        }
    }

    /**
     * Retrieves the stored PMD ruleset.
     * @return the PMD ruleset.
     */
    public String getRuleset() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", RULESET_FILE_NAME);
        try {
            return Files.readString(configPath);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading PMD ruleset file.", e);
        }
    }

    /**
     * Stores the given PMD ruleset.
     * @param newRuleset the PMD ruleset to store.
     */
    public void setRuleset(String newRuleset) {
        if (newRuleset == null || newRuleset.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid ruleset. Cannot be empty.");
        }
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", RULESET_FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, newRuleset);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing PMD ruleset file.", e);
        }
    }

    /**
     * Resets the settings by clearing the stored API key and
     * deleting the API URL file.
     */
    public void resetApiInfo() {
        PasswordSafe.getInstance().set(new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY), null);
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", API_URL_FILE_NAME);
        try {
            Files.deleteIfExists(configPath);
        } catch (IOException e) {
            throw new IllegalStateException("Error deleting API URL file.", e);
        }
    }
}