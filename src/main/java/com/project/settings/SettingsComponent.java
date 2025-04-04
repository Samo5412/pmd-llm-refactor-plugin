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
    private final JTextArea rulesetArea;
    private final JLabel apiKeyStatusLabel;
    private final JLabel infoIconLabel;

    /**
     * Constructs a new SettingsComponent.
     */
    public SettingsComponent() {
        panel = new JPanel(new GridBagLayout());
        apiKeyField = new JPasswordField(20);
        apiUrlField = new JTextField(20);
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
        addVerticalSpace(gbc, 3);
        addApiUrlComponents(gbc);
        addVerticalSpace(gbc, 6);
        addRulesetComponents(gbc);
    }

    /**
     * Adds the API key components to the panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addApiKeyComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        JPanel apiKeyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // 5px horizontal gap, no vertical gap
        JLabel apiKeyLabel = new JLabel("API KEY");
        apiKeyLabel.setFont(labelFont);
        apiKeyPanel.add(apiKeyLabel);
        apiKeyPanel.add(infoIconLabel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // So label + icon stay together
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(apiKeyPanel, gbc);

        // API Key Field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(apiKeyField, gbc);

        // aPI Key Status Label
        gbc.gridy = 2;
        panel.add(apiKeyStatusLabel, gbc);
    }

    /**
     * Adds the API URL components to the panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addApiUrlComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        JLabel apiUrlLabel = new JLabel("API URL");
        apiUrlLabel.setFont(labelFont);
        panel.add(apiUrlLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(apiUrlField, gbc);
    }

    /**
     * Adds the ruleset components to the panel.
     * @param gbc the GridBagConstraints used for layout.
     */
    private void addRulesetComponents(GridBagConstraints gbc) {
        Font labelFont = new Font("Arial", Font.BOLD, 16);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        JLabel rulesetLabel = new JLabel("PMD RULESET");
        rulesetLabel.setFont(labelFont);
        panel.add(rulesetLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(rulesetArea), gbc);
    }

    /**
     * Adds vertical space to the panel.
     * @param gbc the GridBagConstraints used for layout.
     * @param gridy the grid y position to add the space.
     */
    private void addVerticalSpace(GridBagConstraints gbc, int gridy) {
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(Box.createVerticalStrut(10), gbc);
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