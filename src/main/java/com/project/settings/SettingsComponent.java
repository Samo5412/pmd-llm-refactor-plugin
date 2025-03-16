package com.project.settings;

import javax.swing.*;
import java.awt.*;

/**
 * Represents the settings component UI for the application.
 * @author Micael Olsson
 */
public class SettingsComponent {
    private final JPanel panel;
    private final JPasswordField apiKeyField;
    private final JTextField apiUrlField;
    private final JTextArea rulesetArea;
    private final JLabel apiKeyStatusLabel;
    private final JLabel infoIconLabel;

    /**
     * Constructs a new SettingsComponent.
     */
    public SettingsComponent() {
        panel = new JPanel();
        apiKeyField = new JPasswordField(5);
        apiUrlField = new JTextField(5);
        rulesetArea = new JTextArea(10, 30);
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
        Dimension textFieldSize = new Dimension(
                apiKeyField.getPreferredSize().width, apiKeyField.getPreferredSize().height);
        apiKeyField.setPreferredSize(textFieldSize);
        apiUrlField.setPreferredSize(textFieldSize);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        addComponentsToPanel();
    }

    /**
     * Adds components to the main panel.
     */
    private void addComponentsToPanel() {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel apiKeyLabel = new JLabel("API KEY");
        apiKeyLabel.setFont(labelFont);
        apiKeyPanel.add(apiKeyLabel);
        apiKeyPanel.add(infoIconLabel);

        panel.add(apiKeyPanel);
        panel.add(apiKeyField);
        panel.add(apiKeyStatusLabel);

        JPanel apiUrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel apiUrlLabel = new JLabel("API URL");
        apiUrlLabel.setFont(labelFont);
        apiUrlPanel.add(apiUrlLabel);

        panel.add(apiUrlPanel);
        panel.add(apiUrlField);

        JPanel rulesetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel rulesetLabel = new JLabel("PMD RULESET");
        rulesetLabel.setFont(labelFont);
        rulesetPanel.add(rulesetLabel);

        panel.add(rulesetPanel);
        panel.add(new JScrollPane(rulesetArea));
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
        apiKeyField.setText(null);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKeyStatusLabel.setText("API key: NOT SET.");
        } else {
            apiKeyStatusLabel.setText("API key: SET.");
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
     * Returns the ruleset field value.
     * @return the ruleset field value.
     */
    public String getRulesetField() {
        return rulesetArea.getText();
    }

    /**
     * Sets the ruleset field value.
     * @param ruleset the ruleset to set.
     */
    public void setRulesetField(String ruleset) {
        rulesetArea.setText(ruleset);
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