package com.project.ui.settings;

import com.intellij.openapi.options.Configurable;
import com.project.util.LoggerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides the configuration settings for the PMD-LLM-Refactor plugin.
 */
public class SettingsConfiguration implements Configurable {

    /**
     * The settings component UI.
     */
    private SettingsComponent settingsComponent;

    /**
     * The settings manager instance.
     */
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
            settingsComponent.disableApiUrlField();
        }
        return settingsComponent.getPanel();
    }

    /**
     * Loads the settings from the settings manager.
     */
    private void loadSettings() {
        try {
            String key = SettingsManager.getInstance().fetchApiKey();
            settingsComponent.setApiKey(key);
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
            return !settingsComponent.getApiKeyField().equals(settingsManager.fetchApiKey()) ||
                    !settingsComponent.getApiUrlField().equals(settingsManager.getApiUrl()) ||
                    !settingsComponent.getRulesetField().equals(settingsManager.getRuleset()) ||
                    !settingsComponent.getModelNameField().equals(settingsManager.getModelName()) ||
                    !settingsComponent.getTemperatureField().equals(settingsManager.getTemperature()) ||
                    !settingsComponent.getTokenAmountField().equals(settingsManager.getTokenAmount()) ||
                    settingsComponent.isClearRefactorModePreference() != settingsManager.isClearRefactorModePreference();
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
            String newRuleset = settingsComponent.getRulesetField();
            String newModelName = settingsComponent.getModelNameField();
            String newTemperature = settingsComponent.getTemperatureField();
            String newTokenAmount = settingsComponent.getTokenAmountField();
            settingsManager.setClearRefactorModePreference(settingsComponent.isClearRefactorModePreference());

            if (isApiKeyChanged(newApiKey)) {
                settingsComponent.showApiKeyUpdatedNotification();
            }

            settingsManager.setApiKey(newApiKey.isEmpty() ? null : newApiKey);
            settingsManager.setRuleset(newRuleset.isEmpty() ? null : newRuleset);
            settingsManager.setModelName(newModelName.isEmpty() ? null : newModelName);
            settingsManager.setTemperature(newTemperature.isEmpty() ? null : newTemperature);
            settingsManager.setTokenAmount(newTokenAmount.isEmpty() ? null : newTokenAmount);
            settingsManager.processClearRefactorModePreference();
            settingsComponent.setClearRefactorModePreference(settingsManager.isClearRefactorModePreference());

            loadSettings();
        } catch (Exception e) {
            LoggerUtil.error("Error applying settings", e);
        }
    }

    /**
     * Checks if the API key has changed.
     *
     * @param newApiKey The new API key.
     * @return true if the API key has changed, false otherwise.
     */
    private boolean isApiKeyChanged(String newApiKey) {
        try {
            String currentApiKey = settingsManager.fetchApiKey();
            return !currentApiKey.isEmpty() && !currentApiKey.equals(newApiKey);
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
            settingsComponent.disableApiUrlField();
            settingsComponent.getPanel().revalidate();
            settingsComponent.getPanel().repaint();
            settingsComponent.setClearRefactorModePreference(settingsManager.isClearRefactorModePreference());
        } catch (Exception e) {
            LoggerUtil.error("Error resetting settings", e);
        }
    }
}