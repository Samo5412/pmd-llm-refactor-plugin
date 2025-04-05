package com.project.settings;

import com.intellij.openapi.options.Configurable;
import com.project.util.LoggerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides the configuration settings for the PMD-LLM-Refactor plugin.
 */
public class SettingsConfiguration implements Configurable {

    private SettingsComponent settingsComponent;
    private final SettingsManager settingsManager = SettingsManager.getInstance();

    /**
     * Returns the display name of the configuration.
     * @return the display name.
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "PMD-LLM-Refactor: Plugin Configuration";
    }

    /**
     * Creates the settings component UI.
     * @return the settings component UI.
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        if (settingsComponent == null) {
            settingsComponent = new SettingsComponent();
            loadSettings();
        }
        return settingsComponent.getPanel();
    }

    /**
     * Loads the settings from the settings manager.
     */
    private void loadSettings() {
        try {
            settingsComponent.setApiKey(settingsManager.getApiKey());
            settingsComponent.setApiUrlField(settingsManager.getApiUrl());
            settingsComponent.setRulesetField(settingsManager.getRuleset());
            settingsComponent.setModelNameField(settingsManager.getModelName());
            settingsComponent.setTemperatureField(settingsManager.getTemperature());
            settingsComponent.setTokenAmountField(settingsManager.getTokenAmount());
        } catch (Exception e) {
            LoggerUtil.error("Error loading settings", e);
        }
    }

    /**
     * Checks if the settings have been modified.
     *
     * @return true if the settings have been modified, false otherwise.
     */
    @Override
    public boolean isModified() {
        try {
            return !settingsComponent.getApiKeyField().equals(settingsManager.getApiKey()) ||
                    !settingsComponent.getApiUrlField().equals(settingsManager.getApiUrl()) ||
                    !settingsComponent.getRulesetField().equals(settingsManager.getRuleset()) ||
                    !settingsComponent.getModelNameField().equals(settingsManager.getModelName()) ||
                    !settingsComponent.getTemperatureField().equals(settingsManager.getTemperature()) ||
                    !settingsComponent.getTokenAmountField().equals(settingsManager.getTokenAmount());
        } catch (Exception e) {
            LoggerUtil.error("Error checking if settings are modified", e);
            return false;
        }
    }

    /**
     * Applies the modified settings.
     */
    @Override
    public void apply() {
        try {
            String newApiKey = settingsComponent.getApiKeyField();
            String newApiUrl = settingsComponent.getApiUrlField();
            String newRuleset = settingsComponent.getRulesetField();
            String newModelName = settingsComponent.getModelNameField();
            String newTemperature = settingsComponent.getTemperatureField();
            String newTokenAmount = settingsComponent.getTokenAmountField();

            if (isApiKeyChanged(newApiKey)) {
                settingsComponent.showApiKeyUpdatedNotification();
            }

            settingsManager.setApiKey(newApiKey.isEmpty() ? null : newApiKey);
            settingsManager.setApiUrl(newApiUrl.isEmpty() ? null : newApiUrl);
            settingsManager.setRuleset(newRuleset.isEmpty() ? null : newRuleset);
            settingsManager.setModelName(newModelName.isEmpty() ? null : newModelName);
            settingsManager.setTemperature(newTemperature.isEmpty() ? null : newTemperature);
            settingsManager.setTokenAmount(newTokenAmount.isEmpty() ? null : newTokenAmount);

            loadSettings();
        } catch (Exception e) {
            LoggerUtil.error("Error applying settings", e);
        }
    }

    private boolean isApiKeyChanged(String newApiKey) {
        try {
            String currentApiKey = settingsManager.getApiKey();
            return currentApiKey != null && !currentApiKey.isEmpty() && !currentApiKey.equals(newApiKey);
        } catch (Exception e) {
            LoggerUtil.error("Error checking if API key is changed", e);
            return false;
        }
    }

    /**
     * Resets the settings to their original values.
     */
    @Override
    public void reset() {
        try {
            loadSettings();
            settingsComponent.getPanel().revalidate();
            settingsComponent.getPanel().repaint();
        } catch (Exception e) {
            LoggerUtil.error("Error resetting settings", e);
        }
    }
}