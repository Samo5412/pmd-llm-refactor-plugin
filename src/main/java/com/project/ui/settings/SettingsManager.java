package com.project.ui.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.project.util.LoggerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the settings for the application, including API key, API URL, and PMD ruleset.
 * @author Micael Olsson
 */
@Service
@State(
        name = "com.project.settings.ApplicationSettings",
        storages = @Storage("projectSettings.xml")
)
public final class SettingsManager implements PersistentStateComponent<SettingsManager> {

    private static final String CREDENTIAL_SERVICE_NAME = "com.project.admin";
    private static final String API_KEY = "api_key";
    private static final String RULESET_FILE_NAME = "pmd_ruleset.xml";
    private String modelName;
    private String temperature;
    private String tokenAmount;

    /**
     * The API key used for authentication.
     */
    @Transient
    private final AtomicReference<String> cachedApiKey = new AtomicReference<>("");

    /**
     * Returns the singleton instance of the SettingsManager.
     * @return the singleton instance.
     */
    public static SettingsManager getInstance() {
        return ApplicationManager.getApplication().getService(SettingsManager.class);
    }

    /**
     * Initializes the SettingsManager component.
     */
    @Override
    public void initializeComponent() {
        loadApiKeyAsync();
    }

    /**
     * Asynchronously loads the API key from the password safe.
     */
    public void loadApiKeyAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            CredentialAttributes attributes = new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY);
            Credentials credentials = PasswordSafe.getInstance().get(attributes);
            String key = (credentials == null || credentials.getPasswordAsString() == null) ?
                    "" : credentials.getPasswordAsString();
            cachedApiKey.set(key);
        });
    }

    /**
     * Retrieves the stored API key.
     * @return the API key.
     */
    public String fetchApiKey() {
        String key = cachedApiKey.get();
        if (key == null) {
            key = retrieveApiKeyFromPasswordSafe();
            cachedApiKey.set(key);
        }
        return key;
    }


    /**
     * Helper method to retrieve API key from password safe.
     * @return the API key or empty string if not set.
     */
    private String retrieveApiKeyFromPasswordSafe() {
        CredentialAttributes attributes = new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        String key = (credentials == null || credentials.getPasswordAsString() == null) ?
                "" : credentials.getPasswordAsString();

        if (!key.isEmpty()) {
            cachedApiKey.set(key);
        }

        return key;
    }


    /**
     * Stores the given API key.
     * @param newApiKey the API key to store.
     */
    public void setApiKey(String newApiKey) {
        if (newApiKey == null) {
            newApiKey = "";
        }

        cachedApiKey.set(newApiKey);

        String keyToStore = newApiKey;
        ApplicationManager.getApplication().executeOnPooledThread(() -> storeApiKeyInPasswordSafe(keyToStore));
    }

    /**
     * Helper method to store API key in password safe.
     * Should be called from a background thread.
     */
    private void storeApiKeyInPasswordSafe(String apiKey) {
        CredentialAttributes attributes = new CredentialAttributes(CREDENTIAL_SERVICE_NAME + "." + API_KEY);
        Credentials credentials = new Credentials(null, apiKey);
        PasswordSafe.getInstance().set(attributes, credentials);
    }

    /**
     * Retrieves the stored API URL.
     * @return the API URL or default if not set.
     */
    public String getApiUrl() {
        return "https://openrouter.ai/api/v1";
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
     * @return the model name or null if not set.
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Stores the given model name.
     * @param newModelName the model name to store.
     */
    public void setModelName(String newModelName) {
        this.modelName = newModelName;
    }

    /**
     * Retrieves the stored temperature.
     * @return the temperature or null if not set.
     */
    public String getTemperature() {
        return temperature;
    }

    /**
     * Stores the given temperature.
     * @param newTemperature the temperature to store.
     */
    public void setTemperature(String newTemperature) {
        this.temperature = newTemperature;
    }

    /**
     * Retrieves the stored token amount.
     * @return the token amount or null if not set.
     */
    public String getTokenAmount() {
        return tokenAmount;
    }

    /**
     * Stores the given token amount.
     * @param newTokenAmount the token amount to store.
     */
    public void setTokenAmount(String newTokenAmount) {
        this.tokenAmount = newTokenAmount;
    }

    /**
     * Validates the temperature value and shows notifications if invalid
     * @param project Current project for notifications
     * @return ValidationResult with parsed temperature if valid
     */
    public ValidationSettings validateTemperature(Project project) {
        String temperatureStr = getTemperature();
        if (temperatureStr == null || temperatureStr.isEmpty()) {
            showSettingsRequiredNotification(project, "Temperature value is required", "Please set a temperature value in settings.");
            return ValidationSettings.invalid("Temperature value is required");
        }

        try {
            double temperature = Double.parseDouble(temperatureStr);
            return ValidationSettings.valid(temperature);
        } catch (NumberFormatException e) {
            String errorMsg = "Invalid temperature in settings: \"" + temperatureStr +
                    "\". Please enter a valid decimal number in Settings.";
            LoggerUtil.error(errorMsg, e);
            showSettingsRequiredNotification(project, "Invalid Temperature", errorMsg);
            return ValidationSettings.invalid(errorMsg);
        }
    }

    /**
     * Validates the token amount value and shows notifications if invalid
     * @param project Current project for notifications
     * @return ValidationResult with parsed token amount if valid
     */
    public ValidationSettings validateTokenAmount(Project project) {
        String tokenAmountStr = getTokenAmount();
        if (tokenAmountStr == null || tokenAmountStr.isEmpty()) {
            showSettingsRequiredNotification(project, "Token amount is required", "Please set a token amount value in settings.");
            return ValidationSettings.invalid("Token amount is required");
        }

        try {
            int maxTokens = Integer.parseInt(tokenAmountStr);
            return ValidationSettings.valid(maxTokens);
        } catch (NumberFormatException e) {
            String errorMsg = "Invalid token amount in settings: \"" + tokenAmountStr +
                    "\". Please enter a valid integer in Settings.";
            LoggerUtil.error(errorMsg, e);
            showSettingsRequiredNotification(project, "Invalid Token Amount", errorMsg);
            return ValidationSettings.invalid(errorMsg);
        }
    }

    /**
     * Validates the model name and shows notifications if invalid
     * @param project Current project for notifications
     * @return ValidationResult with model name if valid
     */
    public ValidationSettings validateModelName(Project project) {
        String modelName = getModelName();
        if (modelName == null || modelName.isEmpty()) {
            showSettingsRequiredNotification(project, "Model name is required", "Please set a model name in settings.");
            return ValidationSettings.invalid("Model name is required");
        }
        return ValidationSettings.valid(modelName);
    }

    /**
     * Validates the API key and shows notifications if invalid
     * @param project Current project for notifications
     * @return ValidationResult with API key if valid
     */
    public ValidationSettings validateApiKey(Project project) {
        String apiKey = fetchApiKey();
        if (apiKey.isEmpty()) {
            showSettingsRequiredNotification(project, "API Key is required", "Please set an API key in settings.");
            return ValidationSettings.invalid("API Key is required");
        }
        return ValidationSettings.valid(apiKey);
    }

    /**
     * Shows a notification to the user with a link to open settings
     * @param project Current project
     * @param title Notification title
     * @param message Notification message
     */
    private void showSettingsRequiredNotification(Project project, String title, String message) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationGroupManager.getInstance()
                .getNotificationGroup("PMD LLM Refactor")
                .createNotification(message, NotificationType.ERROR)
                .setTitle(title)
                .addAction(NotificationAction.createSimple("Open settings", () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfiguration.class)))
                .notify(project));
    }

    /**
     * Retrieves the current state of the SettingsManager instance.
     *
     * @return the current SettingsManager instance.
     */
    @Override
    public @NotNull SettingsManager getState() {
        return this;
    }

    /**
     * Replaces the current state of this instance with the provided state.
     *
     * @param state the new state to load into this instance; must not be null.
     */
    @Override
    public void loadState(@NotNull SettingsManager state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}