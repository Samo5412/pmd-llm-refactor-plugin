package com.project.ui;

import com.github.javaparser.JavaParser;
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
import com.project.api.ApiClient;
import com.project.api.LLMService;
import com.project.logic.*;
import com.project.model.BatchPreparationResult;
import com.project.model.CodeBlockInfo;
import com.project.settings.SettingsManager;
import com.project.util.LoggerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
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
     * Button to trigger PMD code analysis
     */
    private JButton pmdButton;

    /**
     * Panel to display feedback buttons
     */
    private JPanel feedbackPanel;

    /**
     * The batch preparation result from the last analysis.
     */
    private BatchPreparationResult lastBatchResult;

    /**
     * Map to store analysis results for each analyzed file path.
     * Key: File path, Value: true if issues were found, false otherwise
     */
    private final Map<String, Boolean> analyzedFilesCache = new HashMap<>();

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

        resultTextArea = new JTextArea(10, 40);
        resultTextArea.setEditable(false);
        JScrollPane scrollPane = new JBScrollPane(resultTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        pmdButton = new JButton("Analyse Code");
        pmdButton.setEnabled(false);  // Initially disabled
        pmdButton.addActionListener(e -> runPMDAnalysis(project));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(pmdButton);

        feedbackPanel = UserFeedback.createFeedbackPanel();

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(feedbackPanel, BorderLayout.SOUTH);

        panel.add(southPanel, BorderLayout.SOUTH);

        updateFileStatus(project);

        return panel;
    }

    /**
     * Updates the file status and button based on the currently detected Java file.
     *
     * @param project The current IntelliJ project.
     */
    private void updateFileStatus(Project project) {
        Optional<VirtualFile> fileOpt = FileDetector.detectCurrentJavaFile(project);

        if (fileOpt.isEmpty()) {
            setFileStatus("No Java file detected", false);
            return;
        }

        VirtualFile file = fileOpt.get();
        String filePath = file.getPath();

        // Check if this file was previously analyzed
        if (analyzedFilesCache.containsKey(filePath)) {
            boolean hasIssues = analyzedFilesCache.get(filePath);
            setFileStatus("Java file detected: " + file.getName(), true);

            if (hasIssues) {
                // show the LLM response button
                updateButtonForLLMResponse(project);
            } else {
                // show analyze button
                resetToAnalyzeMode(project);
            }
        } else {
            // File not analyzed yet, show analyze button
            setFileStatus("Java file detected: " + file.getName(), true);
            resetToAnalyzeMode(project);
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
        feedbackPanel.setVisible(false);
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

        String filePath = file.getPath();

        SwingUtilities.invokeLater(() -> {
            // Create component instances
            PMDRunner pmdRunner = new PMDRunner();
            ViolationExtractor violationExtractor = new ViolationExtractor();
            CodeParser codeParser = new CodeParser(new JavaParser());
            ResponseFormatter responseFormatter = new ResponseFormatter();

            // Run analysis
            PMDAnalyzer pmdAnalyzer = new PMDAnalyzer(pmdRunner, violationExtractor, codeParser, responseFormatter);
            String resultMessage = pmdAnalyzer.analyzeFile(project, file);
            updateAnalysisResults(resultMessage);

            boolean hasIssues = !resultMessage.equals("No issues found.");
            // Store the analysis result in cache
            analyzedFilesCache.put(filePath, hasIssues);

            if (hasIssues) {
                lastBatchResult = prepareBatches(filePath, pmdRunner, violationExtractor, codeParser);
                updateButtonForLLMResponse(project);
            } else {
                resetToAnalyzeMode(project);
            }
        });
    }

    /**
     * Updates UI with analysis results.
     *
     * @param resultMessage The message to display in the result area
     */
    private void updateAnalysisResults(String resultMessage) {
        resultTextArea.setText(resultMessage);
        statusLabel.setText("PMD Analysis completed!");
        feedbackPanel.setVisible(true);
    }

    /**
     * Prepares code block batches for LLM processing.
     *
     * @param filePath Path to the file being analyzed
     * @param pmdRunner PMD tool runner instance
     * @param violationExtractor Violation extraction utility
     * @param codeParser Code parsing utility
     * @return BatchPreparationResult with the batches and related information
     */
    private BatchPreparationResult prepareBatches(String filePath, PMDRunner pmdRunner,
                                                  ViolationExtractor violationExtractor,
                                                  CodeParser codeParser) {
        return PromptBatchTrimmer.splitIntoBatches(
                codeParser.extractViolatedBlocksInfo(
                        filePath,
                        violationExtractor.extractViolations(
                                pmdRunner.runPMD(filePath)
                        )
                )
        );
    }

    /**
     * Updates the PMD button to handle LLM response requests.
     *
     * @param project The current IntelliJ project
     */
    private void updateButtonForLLMResponse(Project project) {
        pmdButton.setText("Get LLM Response");
        pmdButton.setEnabled(true);

        pmdButton.addActionListener(e -> {
            try {
                // Show loading dialog
                showLLMProcessingDialog(project);

                configureApiClient();

                String formattedPrompt = createFormattedPrompt();

                // Define model and parameters
                String model = "deepseek/deepseek-chat:free";
                int maxTokens = 1000;
                double temperature = 0.7;

                // Get LLM response
                String llmResponse = LLMService.getLLMResponse(formattedPrompt, model, maxTokens, temperature);
                LoggerUtil.info("LLM response: " + llmResponse);

                // Reset button for new analysis
                resetToAnalyzeMode(project);

            } catch (Exception ex) {
                handleUpdateButtonForLLMResponseError(ex);
            }
        });
    }

    /**
     * Configures the API client to use settings manager instead of environment variables.
     *
     * @throws Exception if an unexpected error occurs during configuration
     */
    private void configureApiClient() throws Exception {
        try {
            SettingsManager settingsManager = SettingsManager.getInstance();
            ApiClient.setUseEnvLoader(false);
            ApiClient.setAdminSettingsManager(settingsManager);
        } catch (Exception ex) {
            ApiClient.setUseEnvLoader(false);
        }
    }

    /**
     * Creates a formatted prompt for the LLM API request.
     *
     * @return The formatted prompt string
     */
    private String createFormattedPrompt() {
        if (!lastBatchResult.batches().isEmpty()) {
            java.util.List<CodeBlockInfo> firstBatch = lastBatchResult.batches().get(0);
            ResponseFormatter formatter = new ResponseFormatter();
            return formatter.formatApiResponse(firstBatch);
        }
        return "";
    }

    /**
     * Handles errors that occur during LLM response retrieval.
     *
     * @param ex The exception that occurred
     */
    private void handleUpdateButtonForLLMResponseError(Exception ex) {
        String errorMessage = "Error getting LLM response: " + ex.getMessage();
        updateAnalysisResults(errorMessage);
        LoggerUtil.error(errorMessage, ex);
    }
    /**
     * Shows a dialog while processing LLM requests.
     *
     * @param project The current IntelliJ project
     */
    private void showLLMProcessingDialog(Project project) {
        JDialog loadingDialog = createLoadingDialog();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(2000); // Simulate processing
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                showBatchResultMessage();
                resetToAnalyzeMode(project);
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }

    /**
     * Creates and configures the loading dialog.
     *
     * @return Configured JDialog for loading indication
     */
    private JDialog createLoadingDialog() {
        JDialog loadingDialog = new JDialog();
        loadingDialog.setTitle("Sending to LLM...");
        loadingDialog.setModal(true);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setSize(350, 100);
        loadingDialog.setLocationRelativeTo(null);
        loadingDialog.setLayout(new BorderLayout());

        JLabel label = new JLabel("Sending code for refactoring...");
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        loadingDialog.add(label, BorderLayout.CENTER);

        return loadingDialog;
    }

    /**
     * Shows a message about batch processing results if needed.
     */
    private void showBatchResultMessage() {
        boolean shouldShowMessage = lastBatchResult.batches().size() > 1
                || !lastBatchResult.skippedBlocks().isEmpty();

        if (shouldShowMessage) {
            JOptionPane.showMessageDialog(
                    null,
                    lastBatchResult.userMessage(),
                    "Refactoring Preparation Complete",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * Resets the UI to the initial state for a new analysis.
     *
     * @param project The current IntelliJ project.
     */
    private void resetToAnalyzeMode(Project project) {
        pmdButton.setText("Analyse Code");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> runPMDAnalysis(project));
    }
}