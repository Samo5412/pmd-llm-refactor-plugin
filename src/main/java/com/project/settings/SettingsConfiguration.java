package com.project.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides the configuration settings for the PMD-LLM-Refactor plugin.
 * @author Micael Olsson
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
        settingsComponent.setApiKey(settingsManager.getApiKey());
        settingsComponent.setApiUrlField(settingsManager.getApiUrl());
        settingsComponent.setRulesetField(settingsManager.getRuleset());
    }

    /**
     * Checks if the settings have been modified.
     * @return true if the settings have been modified, false otherwise.
     */
    @Override
    public boolean isModified() {
        return !settingsComponent.getApiKeyField().isEmpty() ||
                !settingsComponent.getApiUrlField().equals(settingsManager.getApiUrl()) ||
                !settingsComponent.getRulesetField().equals(settingsManager.getRuleset());
    }

    /**
     * Applies the modified settings.
     */
    @Override
    public void apply() {
        String newApiKey = settingsComponent.getApiKeyField();
        String newApiUrl = settingsComponent.getApiUrlField();
        String newRuleset = settingsComponent.getRulesetField();

        if (isApiKeyChanged(newApiKey)) {
            settingsComponent.showApiKeyUpdatedNotification();
        }

        settingsManager.setApiKey(newApiKey.isEmpty() ? null : newApiKey);
        settingsManager.setApiUrl(newApiUrl.isEmpty() ? null : newApiUrl);
        settingsManager.setRuleset(newRuleset.isEmpty() ? null : newRuleset);

        loadSettings();
    }

    private boolean isApiKeyChanged(String newApiKey) {
        String currentApiKey = settingsManager.getApiKey();
        return currentApiKey != null && !currentApiKey.isEmpty() && !currentApiKey.equals(newApiKey);
    }

    /**
     * Resets the settings to their original values.
     */
    @Override
    public void reset() {
        loadSettings();
        settingsComponent.getPanel().revalidate();
        settingsComponent.getPanel().repaint();
    }
}