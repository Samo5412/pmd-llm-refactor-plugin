package com.project.ui.core;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * A Dialog to allow users to select a preferred refactoring mode.
 * Options include LLM Only, Hybrid, or both. Users can optionally save their choice.
 *
 * @author Sara Moussa.
 */
public class RefactorModeDialog extends DialogWrapper {

    /**
     * A radio button for the LLM-only refactor mode.
     */
    private JRadioButton llmOnlyButton;
    /**
     * Radio button allowing the user to select a mode that combines both available refactoring options.
     */
    private JRadioButton bothButton;

    /**
     * A radio button for the hybrid refactor mode.
     */
    private JRadioButton hybridButton;

    /**
     * Checkbox allowing the user to specify whether their refactoring choice
     * should be remembered for future sessions.
     */
    private JCheckBox rememberChoiceCheckbox;
    /**
     * The current project associated with this dialog.
     */
    private final Project project;
    /**
     * Key used for storing and retrieving the selected refactoring mode in settings or preferences.
     */
    public static final String REFACTOR_MODE_KEY = "com.project.refactor.mode";
    /**
     * Key used to store and retrieve the user's preference for remembering the selected refactoring mode.
     */
    public static final String REMEMBER_CHOICE_KEY = "com.project.refactor.remember";

    /**
     * Constructs a dialog for selecting a refactoring mode.
     *
     * @param project The associated project, or null if no project context is available.
     */
    public RefactorModeDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Select Refactoring Mode");
        init();
    }

    /**
     * Creates the central panel of the dialog.
     *
     * @return A JPanel combining the description, radio buttons, and checkbox,
     * or null if no panel is created.
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialogPanel = new JPanel(new BorderLayout());

        // Create panel components
        JPanel radioPanel = createRadioButtonPanel();
        JPanel checkboxPanel = createRememberChoicePanel();
        JPanel descriptionPanel = createDescriptionPanel();

        // Assemble the main dialog panel
        dialogPanel.add(descriptionPanel, BorderLayout.NORTH);
        dialogPanel.add(radioPanel, BorderLayout.CENTER);
        dialogPanel.add(checkboxPanel, BorderLayout.SOUTH);

        return dialogPanel;
    }

    /**
     * Creates the panel containing radio buttons for refactoring modes.
     * @return Panel with radio buttons
     */
    private JPanel createRadioButtonPanel() {
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));

        // Create radio buttons
        hybridButton = new JRadioButton("Hybrid");
        llmOnlyButton = new JRadioButton("LLM Only");
        bothButton = new JRadioButton("Both");

        // Create button group
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(hybridButton);
        buttonGroup.add(llmOnlyButton);
        buttonGroup.add(bothButton);

        // Apply previously saved selection
        applyRefactorModeSelection(hybridButton);

        radioPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add radio buttons to panel
        radioPanel.add(hybridButton);
        radioPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        radioPanel.add(llmOnlyButton);
        radioPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        radioPanel.add(bothButton);

        return radioPanel;
    }

    /**
     * Applies the saved refactor mode selection from properties.
     * @param hybridButton The hybrid mode button (default selection)
     */
    private void applyRefactorModeSelection(JRadioButton hybridButton) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String savedMode = propertiesComponent.getValue(REFACTOR_MODE_KEY);

        if (savedMode != null) {
            switch (savedMode) {
                case "LLM_ONLY":
                    llmOnlyButton.setSelected(true);
                    break;
                case "BOTH":
                    bothButton.setSelected(true);
                    break;
                default:
                    hybridButton.setSelected(true);
                    break;
            }
        } else {
            // Default to Hybrid if no saved preference
            hybridButton.setSelected(true);
        }
    }

    /**
     * Creates the panel containing the remember choice checkbox and information about clearing preferences.
     * @return Panel with remember choice checkbox and info label
     */
    private JPanel createRememberChoicePanel() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

        checkboxPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Panel for checkbox
        JPanel checkboxRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rememberChoiceCheckbox = new JCheckBox("Remember my choice");
        boolean rememberChoice = propertiesComponent.getBoolean(REMEMBER_CHOICE_KEY, false);
        rememberChoiceCheckbox.setSelected(rememberChoice);
        checkboxRow.add(rememberChoiceCheckbox);

        // Panel for help text
        JPanel helpRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel helpLabel = new JLabel("To clear saved preferences, visit Settings → Tools → PMD-LLM-Refactor: Plugin Configuration");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, helpLabel.getFont().getSize() - 1f));
        helpLabel.setForeground(JBColor.GRAY);
        helpRow.add(helpLabel);

        checkboxPanel.add(checkboxRow);
        checkboxPanel.add(helpRow);

        return checkboxPanel;
    }

    /**
     * Creates the panel containing the description text.
     * @return Panel with description label
     */
    private JPanel createDescriptionPanel() {
        JPanel descriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel descriptionLabel = new JLabel("Choose your preferred refactoring mode:");
        descriptionPanel.add(descriptionLabel);

        return descriptionPanel;
    }

    /**
     * Returns the selected refactoring mode.
     * @return The selected RefactorMode enum value
     */
    public RefactorMode getSelectedMode() {
        RefactorMode selectedMode;

        if (llmOnlyButton.isSelected()) {
            selectedMode = RefactorMode.LLM_ONLY;
        } else if (bothButton.isSelected()) {
            selectedMode = RefactorMode.BOTH;
        } else if (hybridButton.isSelected()) {
            selectedMode = RefactorMode.HYBRID;
        } else {
            selectedMode = RefactorMode.HYBRID;
        }

        // Save the selection if "Remember my choice" is checked
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        if (rememberChoiceCheckbox.isSelected()) {
            propertiesComponent.setValue(REFACTOR_MODE_KEY, selectedMode.toString());
            propertiesComponent.setValue(REMEMBER_CHOICE_KEY, true);
        } else {
            propertiesComponent.unsetValue(REFACTOR_MODE_KEY);
            propertiesComponent.setValue(REMEMBER_CHOICE_KEY, false);
        }
        return selectedMode;
    }

    /**
     * Checks if there's a remembered choice and what mode it is.
     *
     * @param project The current project
     * @return The remembered mode or null if no mode is remembered
     */
    public static RefactorMode getRememberedMode(Project project) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        boolean remember = propertiesComponent.getBoolean(REMEMBER_CHOICE_KEY, false);

        if (remember) {
            String savedMode = propertiesComponent.getValue(REFACTOR_MODE_KEY);
            if (savedMode != null) {
                try {
                    return RefactorMode.valueOf(savedMode);
                } catch (IllegalArgumentException e) {
                    return RefactorMode.HYBRID;
                }
            }
        }

        return null;
    }
}