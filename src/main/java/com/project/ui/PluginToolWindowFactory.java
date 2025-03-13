package com.project.ui;

import com.github.javaparser.JavaParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.project.logic.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Factory class for creating a tool window in the IntelliJ plugin.
 * Displays detected Java files and allows code analysis execution.
 *
 * @author Sara Moussa
 */
public class PluginToolWindowFactory implements ToolWindowFactory {

    /**
     * Label to display the status of file detection
     */
    private JBLabel statusLabel;

    /**
     * Text area to show analysis results
     */
    private JTextArea resultTextArea;

    /**
     * Button to trigger PMD code analysis
     */
    private JButton pmdButton;

    /**
     * Initializes the tool window UI and registers event listeners.
     *
     * @param project    The current IntelliJ project.
     * @param toolWindow The tool window instance.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (toolWindow.getContentManager().getContentCount() > 0) {
            return;
        }

        JPanel panel = createMainPanel(project);

        // Register listener to update the status when a new Java file is detected
        FileDetector.registerFileListener(project, () -> updateFileStatus(project));

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Creates and returns the main panel for the tool window.
     *
     * @param project The current IntelliJ project.
     * @return The initialized UI panel.
     */
    private JPanel createMainPanel(Project project) {
        JPanel panel = new JPanel(new BorderLayout());

        statusLabel = new JBLabel("Detecting file...");
        panel.add(statusLabel, BorderLayout.NORTH);

        resultTextArea = new JTextArea(10, 40);
        resultTextArea.setEditable(false);
        JScrollPane scrollPane = new JBScrollPane(resultTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        pmdButton = new JButton("Analyse Code");
        pmdButton.setEnabled(false);  // Initially disabled
        pmdButton.addActionListener(e -> runPMDAnalysis(project));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(pmdButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        updateFileStatus(project);

        return panel;
    }

    /**
     * Updates the status label and enables/disables the analysis button.
     *
     * @param project The current IntelliJ project.
     */
    private void updateFileStatus(Project project) {
        FileDetector.detectCurrentJavaFile(project)
                .ifPresentOrElse(
                        file -> setFileStatus("Detected Java file: " + file.getName(), true),
                        () -> setFileStatus("No Java file detected.", false)
                );
    }

    /**
     * Helper method to update the status label and button state.
     *
     * @param statusMessage The message to display in the status label.
     * @param isEnabled     Whether the "Analyze Code" button should be enabled.
     */
    private void setFileStatus(String statusMessage, boolean isEnabled) {
        statusLabel.setText(statusMessage);
        pmdButton.setEnabled(isEnabled);
    }

    /**
     * Runs PMD analysis on the currently detected Java file.
     * Displays results in the text area.
     *
     * @param project The current IntelliJ project.
     */
    private void runPMDAnalysis(Project project) {
        VirtualFile file = FileDetector.detectCurrentJavaFile(project).orElse(null);

        if (file == null) {
            resultTextArea.setText("Error: No Java file selected.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            PMDAnalyzer pmdAnalyzer = new PMDAnalyzer(
                    new PMDRunner(),
                    new ViolationExtractor(),
                    new CodeParser(new JavaParser()),
                    new ResponseFormatter()
            );

            String result = pmdAnalyzer.analyzeFile(project, file);
            resultTextArea.setText(result);
            statusLabel.setText("PMD Analysis completed!");
        });
    }
}
