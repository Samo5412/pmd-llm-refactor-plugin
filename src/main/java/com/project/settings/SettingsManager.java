package com.project.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.PathManager;
import com.project.util.LoggerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages the settings for the application, including API key, API URL, and PMD ruleset.
 * @author Micael Olsson
 */
public class SettingsManager {

    private static final String CREDENTIAL_SERVICE_NAME = "com.project.admin";
    private static final String API_KEY = "api_key";
    private static final String API_URL_FILE_NAME = "api_url.txt";
    private static final String RULESET_FILE_NAME = "pmd_ruleset.xml";
    private static final String MODEL_NAME_FILE_NAME = "model_name.txt";
    private static final String TEMPERATURE_FILE_NAME = "temperature.txt";
    private static final String TOKEN_AMOUNT_FILE_NAME = "token_amount.txt";
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
     * @return the API URL or null if not set.
     */
    public String getApiUrl() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", API_URL_FILE_NAME);
        try {
            if (Files.exists(configPath)) {
                return Files.readString(configPath).trim();
            }
            return null;
        } catch (IOException e) {
            LoggerUtil.error("Error reading API URL file.", e);
            return null;
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
     * @return the PMD ruleset or null if not set.
     */
    public String getRuleset() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", RULESET_FILE_NAME);
        try {
            if (Files.exists(configPath)) {
                return Files.readString(configPath);
            }
            return null;
        } catch (IOException e) {
            LoggerUtil.error("Error reading PMD ruleset file." , e);
            return null;
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
     * Retrieves the stored model name.
     *
     * @return the model name or null if not set.
     */
    public String getModelName() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", MODEL_NAME_FILE_NAME);
        try {
            if (Files.exists(configPath)) {
                return Files.readString(configPath).trim();
            }
            return null;
        } catch (IOException e) {
            LoggerUtil.error("Error reading model name file.", e);
            return null;
        }
    }

    /**
     * Stores the given model name.
     * @param newModelName the model name to store.
     */
    public void setModelName(String newModelName) {
        if (newModelName == null || newModelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid model name, cannot be empty.");
        }
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", MODEL_NAME_FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, newModelName);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing model name file.", e);
        }
    }

    /**
     * Retrieves the stored temperature.
     * @return the temperature or null if not set.
     */
    public String getTemperature() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", TEMPERATURE_FILE_NAME);
        try {
            if (Files.exists(configPath)) {
                return Files.readString(configPath).trim();
            }
            return null;
        } catch (IOException e) {
            LoggerUtil.error("Error reading temperature file.", e);
            return null;
        }
    }

    /**
     * Stores the given temperature.
     * @param newTemperature the temperature to store.
     */
    public void setTemperature(String newTemperature) {
        if (newTemperature == null || newTemperature.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid temperature. Cannot be empty.");
        }
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", TEMPERATURE_FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, newTemperature);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing temperature file.", e);
        }
    }

    /**
     * Retrieves the stored token amount.
     * @return the token amount or null if not set.
     */
    public String getTokenAmount() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", TOKEN_AMOUNT_FILE_NAME);
        try {
            if (Files.exists(configPath)) {
                return Files.readString(configPath).trim();
            }
            return null;
        } catch (IOException e) {
            LoggerUtil.error("Error reading token amount file.", e);
            return null;
        }
    }

    /**
     * Stores the given token amount.
     * @param newTokenAmount the token amount to store.
     */
    public void setTokenAmount(String newTokenAmount) {
        if (newTokenAmount == null || newTokenAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid token amount. Cannot be empty.");
        }
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", TOKEN_AMOUNT_FILE_NAME);
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, newTokenAmount);
        } catch (IOException e) {
            throw new IllegalStateException("Error writing token amount file.", e);
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