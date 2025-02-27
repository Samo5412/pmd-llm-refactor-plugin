package com.project.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.project.logic.FileDetector;
import com.project.logic.PMDAnalyzer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class PMDToolWindowFactory implements ToolWindowFactory {

    private JBLabel statusLabel;
    private JTextArea resultTextArea;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getContentManager().removeAllContents(true);

        JPanel panel = new JPanel(new BorderLayout());


        statusLabel = new JBLabel("Detecting file...");
        panel.add(statusLabel, BorderLayout.NORTH);


        resultTextArea = new JTextArea(10, 40);
        resultTextArea.setEditable(false);
        JScrollPane scrollPane = new JBScrollPane(resultTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);


        JButton pmdButton = new JButton("Run PMD Analysis");
        pmdButton.addActionListener(e -> runPMDAnalysis(project));


        JPanel buttonPanel = new JPanel();
        buttonPanel.add(pmdButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);


        updateFileStatus(project);


        FileDetector.registerFileListener(project, () -> updateFileStatus(project));


        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, null, false);
        toolWindow.getContentManager().addContent(content);
    }

    private void updateFileStatus(Project project) {
        FileDetector.detectCurrentJavaFile(project).ifPresentOrElse(
                file -> statusLabel.setText("Detected Java file: " + file.getName()),
                () -> statusLabel.setText("No Java file detected.")
        );
    }

    private void runPMDAnalysis(Project project) {
        // Get the currently detected Java file
        VirtualFile file = FileDetector.detectCurrentJavaFile(project).orElse(null);

        if (file == null) {
            resultTextArea.setText("Error: No Java file selected.");
            return;
        }

        statusLabel.setText("Running PMD Analysis...");
        resultTextArea.setText("Analyzing " + file.getName() + "...\n");

        // Run analysis
        SwingUtilities.invokeLater(() -> {
            String result = PMDAnalyzer.analyzeFile(project, file);

            resultTextArea.setText(result);
            statusLabel.setText("PMD Analysis completed!");
        });
    }
}
