package com.project.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.project.ui.util.FileAnalysisTracker;
import com.project.ui.util.FileChangeListener;
import com.project.ui.util.FileContentChangeListener;
import com.project.ui.util.FileTrackingManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

/**
 * Factory class for creating a tool window in the IntelliJ plugin.
 * Displays detected Java files and allows code analysis execution.
 *
 * @author Sara Moussa
 */
public class PluginToolWindowFactory implements ToolWindowFactory,
        FileChangeListener,
        FileContentChangeListener {

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
     * Tracks and caches file analysis results.
     */
    private final FileAnalysisTracker fileAnalysisTracker;

    /**
     * Manages file tracking and change notifications.
     */
    private FileTrackingManager fileTrackingManager;

    /**
     * Button to copy the LLM response to the clipboard
     */
    private JButton copyToClipboardButton;

    /**
     * Instance of the AnalysisFeatures class to handle analysis features.
     */
    private AnalysisFeatures analysisFeatures;

    /**
     * Button to regenerate the LLM response by invalidating the cache
     */
    private JButton regenerateLLMButton;


    /**
     * Creates a new instance of the PluginToolWindowFactory.
     */
    public PluginToolWindowFactory() {
        this.fileAnalysisTracker = new FileAnalysisTracker();
    }

    /**
     * Initializes the tool window UI and registers event listeners.
     *
     * @param project    The current IntelliJ project.
     * @param toolWindow The tool window instance.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JPanel contentPanel = createMainPanel();

        // initialize UI components with default states
        statusLabel.setText("No Java file detected");
        resultTextArea.setText("Ready to analyze.");
        llmResponseTextArea.setText("");
        pmdButton.setEnabled(false);
        feedbackPanel.setVisible(false);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(contentPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        analysisFeatures = new AnalysisFeatures(resultTextArea, llmResponseTextArea, fileAnalysisTracker, pmdButton, statusLabel, feedbackPanel);

        // Initialize file tracking manager and register as listener
        fileTrackingManager = new FileTrackingManager(project, fileAnalysisTracker, llmResponseTextArea);
        fileTrackingManager.addFileChangeListener(this);
        fileTrackingManager.addFileContentChangeListener(this);
    }

    /**
     * Handles file change events from the FileTrackingManager.
     *
     * @param file The active Java file, or empty if no Java file is active.
     */
    @Override
    public void onFileChanged(@NotNull Optional<VirtualFile> file) {
        Project project = fileTrackingManager.getProject();

        if (file.isEmpty()) {
            statusLabel.setText("No Java file detected");
            pmdButton.setEnabled(false);
            resultTextArea.setText("");
            llmResponseTextArea.setText("");
            feedbackPanel.setVisible(false);
            regenerateLLMButton.setVisible(false);
            return;
        }

        VirtualFile currentFile = file.get();
        String filePath = currentFile.getPath();

        // Set status label and enable analyze button for all Java files
        statusLabel.setText("Java file detected: " + currentFile.getName());
        pmdButton.setEnabled(true);

        // Check if this file was previously analyzed
        Boolean hasIssues = fileAnalysisTracker.getCachedAnalysisResult(filePath);

        if (hasIssues != null) {
            // Restore PMD results
            String cachedPmdResult = fileAnalysisTracker.getCachedPmdResult(filePath);
            if (cachedPmdResult != null) {
                resultTextArea.setText(cachedPmdResult);
            }

            // Restore LLM response if it exists
            String cachedLLMResponse = fileAnalysisTracker.getCachedLLMResponse(filePath);
            if (cachedLLMResponse != null) {
                llmResponseTextArea.setText(cachedLLMResponse);

                // Check if this was a canceled request
                boolean wasCanceled = cachedLLMResponse.contains("LLM request cancelled");

                if (hasIssues) {
                    if (!wasCanceled && !cachedLLMResponse.isEmpty()) {
                        analysisFeatures.updateButtonToViewDiff(project);
                        feedbackPanel.setVisible(true);
                        regenerateLLMButton.setVisible(true);
                    } else {
                        analysisFeatures.updateButtonForLLMResponse(project);
                        feedbackPanel.setVisible(false);
                    }
                } else {
                    analysisFeatures.resetToAnalyzeMode(project);
                    feedbackPanel.setVisible(false);
                }
            } else {
                regenerateLLMButton.setVisible(false);

                if (hasIssues) {
                    analysisFeatures.updateButtonForLLMResponse(project);
                } else {
                    analysisFeatures.resetToAnalyzeMode(project);
                }
                feedbackPanel.setVisible(false);
            }
        } else {
            resultTextArea.setText("Ready to analyze.");
            llmResponseTextArea.setText("");
            analysisFeatures.resetToAnalyzeMode(project);
            feedbackPanel.setVisible(false);
            regenerateLLMButton.setVisible(false);
        }
    }

    /**
     * Handles file content change events from the FileTrackingManager.
     *
     * @param file The file whose content has changed.
     */
    @Override
    public void onFileContentChanged(@NotNull VirtualFile file) {
        statusLabel.setText("File modified, re-analysis required");

        resultTextArea.setText("");
        llmResponseTextArea.setText("");

        if (analysisFeatures != null) {
            analysisFeatures.resetToAnalyzeMode(fileTrackingManager.getProject());
        } else {
            pmdButton.setEnabled(true);
        }
        feedbackPanel.setVisible(false);
    }


    /**
     * Creates and returns the main panel for the tool window.
     *
     * @return The initialized UI panel.
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        statusLabel = new JBLabel("Detecting file...");
        panel.add(statusLabel, BorderLayout.NORTH);

        JScrollPane resultScrollPane = createResultScrollPane();
        JScrollPane llmResponseScrollPane = createLLMResponseScrollPane();
        JSplitPane splitPane = createSplitPane(resultScrollPane, llmResponseScrollPane);
        panel.add(splitPane, BorderLayout.CENTER);

        JPanel southPanel = createSouthPanel();
        panel.add(southPanel, BorderLayout.SOUTH);

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
        regenerateLLMButton = createRegenerateLLMButton();
        regenerateLLMButton.setVisible(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(regenerateLLMButton);
        buttonPanel.add(copyToClipboardButton);
        layeredPane.add(buttonPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultScrollPane, layeredPane);
        splitPane.setResizeWeight(0.5);
        return splitPane;
    }

    /**
     * Creates a button to regenerate the LLM response.
     * @return The configured JButton for regenerating LLM response.
     */
    private JButton createRegenerateLLMButton() {
        JButton button = new JButton("Regenerate LLM");
        button.addActionListener(e -> {
            if (analysisFeatures != null) {
                analysisFeatures.regenerateLLMResponse(fileTrackingManager.getProject());
            }
        });
        return button;
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
}