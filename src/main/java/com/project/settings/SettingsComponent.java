package com.project.settings;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Represents the settings component UI for the application.
 */
public class SettingsComponent {
    private final JPanel panel;
    private final JPasswordField apiKeyField;
    private final JTextField apiUrlField;
    private final JTextArea rulesetField;
    private final JTextField modelNameField;
    private final JTextField temperatureField;
    private final JTextField tokenAmountField;
    private final JLabel apiKeyStatusLabel;
    private final JLabel infoIconLabel;

    /**
     * Constructs a new SettingsComponent.
     */
    public SettingsComponent() {
        panel = new JPanel(new GridBagLayout());
        apiKeyField = new JPasswordField(20);
        apiUrlField = new JTextField(20);
        modelNameField = new JTextField(20);
        temperatureField = new JTextField(20);
        tokenAmountField = new JTextField(20);
        rulesetField = new JTextArea(10, 30);
        apiKeyStatusLabel = new JLabel("API key: ");
        infoIconLabel = new JLabel(UIManager.getIcon("OptionPane.questionIcon"));
        infoIconLabel.setToolTipText("<html>To enable persisting storage of API key<br>" +
                "between IntelliJ sessions, go to File -> Settings -><br>" +
                "Appearance and Behavior -> System Settings -><br>" +
                "Passwords and uncheck 'Do not save, forget<br>" +
                "passwords after restart'</html>");
        setupUI();
    }

    /**
     * Sets up the UI components.
     */
    private void setupUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        addComponentsToPanel(gbc);
    }

    /**
     * Adds components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addComponentsToPanel(GridBagConstraints gbc) {
        addApiKeyComponents(gbc);
        addApiUrlComponents(gbc);
        addModelNameComponents(gbc);
        addTemperatureComponents(gbc);
        addTokenAmountComponents(gbc);
        addRulesetComponents(gbc);
    }

    /**
     * Adds API key components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addApiKeyComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel apiKeyLabel = new JLabel("Api Key");
        apiKeyLabel.setFont(labelFont);
        apiKeyPanel.add(apiKeyLabel);
        apiKeyPanel.add(infoIconLabel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(apiKeyPanel, gbc);

        // API Key Field
        gbc.gridy = 1;
        panel.add(apiKeyField, gbc);

        // API Key Status Label
        gbc.gridy = 2;
        panel.add(apiKeyStatusLabel, gbc);
    }

    /**
     * Adds API URL components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addApiUrlComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        JLabel apiUrlLabel = new JLabel("Api Url");
        apiUrlLabel.setFont(labelFont);
        panel.add(apiUrlLabel, gbc);

        gbc.gridx = 1;
        panel.add(apiUrlField, gbc);
    }

    /**
     * Adds model name components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addModelNameComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        JLabel modelNameLabel = new JLabel("LLM Model Name");
        modelNameLabel.setFont(labelFont);
        panel.add(modelNameLabel, gbc);

        gbc.gridx = 1;
        panel.add(modelNameField, gbc);
    }

    /**
     * Adds temperature components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addTemperatureComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        JLabel temperatureLabel = new JLabel("Temperature");
        temperatureLabel.setFont(labelFont);
        panel.add(temperatureLabel, gbc);

        gbc.gridx = 1;
        panel.add(temperatureField, gbc);
    }

    /**
     * Adds token amount components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addTokenAmountComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        JLabel tokenAmountLabel = new JLabel("Token Amount");
        tokenAmountLabel.setFont(labelFont);
        panel.add(tokenAmountLabel, gbc);

        gbc.gridx = 1;
        panel.add(tokenAmountField, gbc);
    }

    /**
     * Adds ruleset components to the main panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addRulesetComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        JLabel rulesetLabel = new JLabel("PMD Ruleset");
        rulesetLabel.setFont(labelFont);
        panel.add(rulesetLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(rulesetField), gbc);
    }

    /**
     * Returns the main panel.
     * @return the main panel.
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * Returns the API key field value.
     * @return the API key field value.
     */
    public String getApiKeyField() {
        return new String(apiKeyField.getPassword());
    }

    /**
     * Sets the API key field value.
     * @param apiKey the API key to set.
     */
    public void setApiKey(String apiKey) {
        apiKeyField.setText(apiKey);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKeyStatusLabel.setText("API key: NOT SET");
        } else {
            apiKeyStatusLabel.setText("API key: SET");
        }
    }

    /**
     * Returns the API URL field value.
     * @return the API URL field value.
     */
    public String getApiUrlField() {
        return apiUrlField.getText();
    }

    /**
     * Sets the API URL field value.
     * @param apiUrl the API URL to set.
     */
    public void setApiUrlField(String apiUrl) {
        apiUrlField.setText(apiUrl);
    }

    /**
     * Returns the model name field value.
     * @return the model name field value.
     */
    public String getModelNameField() {
        return modelNameField.getText();
    }

    /**
     * Sets the model name field value.
     * @param modelName the model name to set.
     */
    public void setModelNameField(String modelName) {
        modelNameField.setText(modelName);
    }

    /**
     * Returns the temperature field value.
     * @return the temperature field value.
     */
    public String getTemperatureField() {
        return temperatureField.getText();
    }

    /**
     * Sets the temperature field value.
     * @param temperature the temperature to set.
     */
    public void setTemperatureField(String temperature) {
        temperatureField.setText(temperature);
    }

    /**
     * Returns the token amount field value.
     * @return the token amount field value.
     */
    public String getTokenAmountField() {
        return tokenAmountField.getText();
    }

    /**
     * Sets the token amount field value.
     * @param tokenAmount the token amount to set.
     */
    public void setTokenAmountField(String tokenAmount) {
        tokenAmountField.setText(tokenAmount);
    }

    /**
     * Returns the ruleset field value.
     * @return the ruleset field value.
     */
    public String getRulesetField() {
        return rulesetField.getText();
    }

    /**
     * Sets the ruleset field value.
     * @param ruleset the ruleset to set.
     */
    public void setRulesetField(String ruleset) {
        rulesetField.setText(ruleset);
    }

    /**
     * Displays a notification to the user indicating that a new API key has been set.
     */
    public void showApiKeyUpdatedNotification() {
        JOptionPane.showMessageDialog(panel,
                "A new API key has been set.",
                "API Key Updated",
                JOptionPane.INFORMATION_MESSAGE);
    }
}