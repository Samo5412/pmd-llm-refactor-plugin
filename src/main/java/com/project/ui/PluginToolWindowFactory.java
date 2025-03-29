package com.project.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
     * Text area to display the LLM response.
     */
    private JTextArea llmResponseTextArea;

    /**
     * Button to trigger PMD code analysis
     */
    private JButton pmdButton;

    /**
     * Panel to display feedback buttons
     */
    private JPanel feedbackPanel;

    /**
     * Map to store analysis results for each analyzed file path.
     * Key: File path, Value: true if issues were found, false otherwise
     */
    private final Map<String, Boolean> analyzedFilesCache = new HashMap<>();

    /**
     * Button to copy the LLM response to the clipboard
     */
    private JButton copyToClipboardButton;


    /**
     * Initializes the tool window UI and registers event listeners.
     *
     * @param project    The current IntelliJ project.
     * @param toolWindow The tool window instance.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel contentPanel = createMainPanel(project);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Set initial file status
        updateFileStatus(project);

        // Add file editor listener to detect file switching
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        updateFileStatus(project);
                    }

                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        updateFileStatus(project);
                    }
                }
        );

        // Create a disposable for the document listener
        Disposable disposable = Disposer.newDisposable();
        Disposer.register(project, disposable);

        // track changes to the currently open file
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void documentChanged(@NotNull DocumentEvent event) {
                        Optional<VirtualFile> currentFile = FileDetector.detectCurrentJavaFile(project);
                        if (currentFile.isPresent()) {
                            Document currentDoc = FileDocumentManager.getInstance().getDocument(currentFile.get());
                            if (currentDoc == event.getDocument()) {
                                // File content changed, remove from analyzed cache
                                analyzedFilesCache.remove(currentFile.get().getPath());
                                updateFileStatus(project);
                            }
                        }
                    }
                },
                disposable
        );
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

        JScrollPane resultScrollPane = createResultScrollPane();
        JScrollPane llmResponseScrollPane = createLLMResponseScrollPane();
        JSplitPane splitPane = createSplitPane(resultScrollPane, llmResponseScrollPane);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel southPanel = createSouthPanel();
        panel.add(southPanel, BorderLayout.SOUTH);

        updateFileStatus(project);

        return panel;
    }

    /**
     * Creates and returns a scroll pane for displaying PMD analysis results.
     * @return The initialized JScrollPane for PMD results.
     */
    private JScrollPane createResultScrollPane() {
        resultTextArea = new JTextArea(10, 40);
        resultTextArea.setEditable(false);
        return new JBScrollPane(resultTextArea);
    }

    /**
     * Creates and returns a scroll pane for displaying LLM responses.
     * @return The initialized JScrollPane for LLM responses.
     */
    private JScrollPane createLLMResponseScrollPane() {
        llmResponseTextArea = new JTextArea(10, 40);
        llmResponseTextArea.setEditable(false);
        return new JBScrollPane(llmResponseTextArea);
    }

    /**
     * Creates and returns a split pane containing the result and LLM response scroll panes.
     * @param resultScrollPane The scroll pane for PMD results.
     * @param llmResponseScrollPane The scroll pane for LLM responses.
     * @return The initialized JSplitPane.
     */
    private JSplitPane createSplitPane(JScrollPane resultScrollPane, JScrollPane llmResponseScrollPane) {
        JPanel llmResponsePanel = new JPanel(new BorderLayout());
        JLabel llmResponseLabel = new JLabel("LLM response");
        llmResponsePanel.add(llmResponseLabel, BorderLayout.NORTH);
        llmResponsePanel.add(llmResponseScrollPane, BorderLayout.CENTER);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new BorderLayout());
        layeredPane.add(llmResponsePanel, BorderLayout.CENTER);

        copyToClipboardButton = createCopyToClipboardButton();
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(copyToClipboardButton);
        layeredPane.add(buttonPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultScrollPane, layeredPane);
        splitPane.setResizeWeight(0.5);
        return splitPane;
    }

    /**
     * Creates and returns the south panel containing the analyze button and feedback panel.
     * @return The initialized JPanel for the south section.
     */
    private JPanel createSouthPanel() {
        pmdButton = new JButton("Analyse Code");
        pmdButton.setEnabled(false);

        JPanel buttonPanelBottom = new JPanel();
        buttonPanelBottom.add(pmdButton);

        feedbackPanel = UserFeedback.createFeedbackPanel();
        feedbackPanel.setVisible(false);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanelBottom, BorderLayout.NORTH);
        southPanel.add(feedbackPanel, BorderLayout.SOUTH);

        return southPanel;
    }

    /**
     * Creates a button to copy the LLM response to the clipboard.
     * @return The configured JButton for copying to clipboard.
     */
    private JButton createCopyToClipboardButton() {
        copyToClipboardButton = new JButton("Copy to Clipboard");
        copyToClipboardButton.addActionListener(e -> {
            String llmResponseText = llmResponseTextArea.getText();
            StringSelection stringSelection = new StringSelection(llmResponseText);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        return copyToClipboardButton;
    }

    private void updateFileStatus(Project project) {
        Optional<VirtualFile> fileOpt = FileDetector.detectCurrentJavaFile(project);

        if (fileOpt.isEmpty()) {
            setFileStatus("No Java file detected", false);
            return;
        }

        VirtualFile file = fileOpt.get();
        String filePath = file.getPath();

        AnalysisFeatures analysisFeatures = new AnalysisFeatures(resultTextArea, llmResponseTextArea, analyzedFilesCache, pmdButton, statusLabel, feedbackPanel);

        // Check if this file was previously analyzed
        if (analyzedFilesCache.containsKey(filePath)) {
            boolean hasIssues = analyzedFilesCache.get(filePath);
            setFileStatus("Java file detected: " + file.getName(), true);

            if (hasIssues) {
                // show the LLM response button
                analysisFeatures.updateButtonForLLMResponse(project);
            } else {
                // show analyze button
                analysisFeatures.resetToAnalyzeMode(project);
                feedbackPanel.setVisible(false);
            }
        } else {
            // File not analyzed yet, show analyze button
            setFileStatus("Java file detected: " + file.getName(), true);
            analysisFeatures.resetToAnalyzeMode(project);
            feedbackPanel.setVisible(false);
        }
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
}