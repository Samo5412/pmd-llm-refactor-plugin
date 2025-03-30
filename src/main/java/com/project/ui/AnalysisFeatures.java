package com.project.ui;

import com.github.javaparser.JavaParser;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.api.RequestStorage;
import com.project.logic.*;
import com.project.model.BatchPreparationResult;
import com.project.ui.util.FileAnalysisTracker;
import com.project.util.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Handles both PMD analysis and LLM response logic and UI updates.
 */
public class AnalysisFeatures {

    /**
     * Text areas for displaying PMD analysis results
     */
    private final JTextArea resultTextArea;

    /**
     * Text area for displaying LLM responses.
     */
    private final JTextArea llmResponseTextArea;

    /**
     * Button to trigger PMD analysis or LLM response.
     */
    private final JButton pmdButton;

    /**
     * Label to display the status of the analysis.
     */
    private final JLabel statusLabel;

    /**
     * Panel for displaying user feedback options.
     */
    private final JPanel feedbackPanel;

    /**
     * Tracks and caches file analysis results.
     */
    private final FileAnalysisTracker fileAnalysisTracker;

    /**
     * Stores the last batch preparation result for LLM processing.
     */
    private BatchPreparationResult lastBatchResult;

    public AnalysisFeatures(JTextArea resultTextArea, JTextArea llmResponseTextArea, FileAnalysisTracker fileAnalysisTracker, JButton pmdButton, JLabel statusLabel, JPanel feedbackPanel) {
        this.resultTextArea = resultTextArea;
        this.llmResponseTextArea = llmResponseTextArea;
        this.fileAnalysisTracker = fileAnalysisTracker;
        this.pmdButton = pmdButton;
        this.statusLabel = statusLabel;
        this.feedbackPanel = feedbackPanel;
    }

    /**
     * Runs PMD analysis on the currently detected Java file.
     * Displays results in the text area.
     *
     * @param project The current IntelliJ project.
     */
    public void runPMDAnalysis(Project project) {
        VirtualFile file = FileDetector.detectCurrentJavaFile(project).orElse(null);

        if (file == null) {
            resultTextArea.setText("Error: No Java file selected.");
            return;
        }

        String filePath = file.getPath();

        SwingUtilities.invokeLater(() -> {
            PMDRunner pmdRunner = new PMDRunner();
            ViolationExtractor violationExtractor = new ViolationExtractor();
            CodeParser codeParser = new CodeParser(new JavaParser());
            ResponseFormatter responseFormatter = new ResponseFormatter();

            PMDAnalyzer pmdAnalyzer = new PMDAnalyzer(pmdRunner, violationExtractor, codeParser, responseFormatter);
            String resultMessage = pmdAnalyzer.analyzeFile(project, file);
            updateAnalysisResults(resultMessage);

            boolean hasIssues = !resultMessage.equals("No issues found.");
            fileAnalysisTracker.cacheAnalysisResult(filePath, hasIssues, resultMessage);

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
     * @param resultMessage The message to display in the result area.
     */
    private void updateAnalysisResults(String resultMessage) {
        resultTextArea.setText(resultMessage);
        statusLabel.setText("PMD Analysis completed!");
    }

    /**
     * Prepares code block batches for LLM processing.
     *
     * @param filePath Path to the file being analyzed.
     * @param pmdRunner PMD tool runner instance.
     * @param violationExtractor Violation extraction utility.
     * @param codeParser Code parsing utility.
     * @return BatchPreparationResult with the batches and related information.
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
     * Resets the UI to the initial state for a new analysis.
     *
     * @param project The current IntelliJ project.
     */
    public void resetToAnalyzeMode(Project project) {
        pmdButton.setText("Analyse Code");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> runPMDAnalysis(project));
    }

    /**
     * Updates the PMD button to handle LLM response requests.
     *
     * @param project The current IntelliJ project.
     */
    public void updateButtonForLLMResponse(Project project) {
        pmdButton.setText("Get LLM Response");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> showLLMProcessingDialog(project));
    }

    /**
     * Shows a dialog while processing LLM requests.
     *
     * @param project The current IntelliJ project.
     */
    public void showLLMProcessingDialog(Project project) {
        JDialog loadingDialog = createLoadingDialog();

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                Optional<VirtualFile> fileOpt = FileDetector.detectCurrentJavaFile(project);
                if (fileOpt.isEmpty()) return null;

                String filePath = fileOpt.get().getPath();

                String cachedResponse = fileAnalysisTracker.getCachedLLMResponse(filePath);
                if (cachedResponse != null) {
                    LoggerUtil.info("Cache hit for file: " + filePath);
                    return cachedResponse;
                }

                try {
                    Thread.sleep(2000);  // simulate processing time for LLM
                    String llmResponse = "public class HelloWorld {\n" + // TODO: Replace with actual LLM response
                            "    public static void main(String[] args) {\n" +
                            "        System.out.println(\"Hello, World!\");\n" +
                            "    }\n" +
                            "}";

                    LoggerUtil.info("LLM response received: " + llmResponse);
                    fileAnalysisTracker.cacheLLMResponse(filePath, llmResponse);
                    return llmResponse;
                } catch (InterruptedException e) {
                    LoggerUtil.error("Error processing LLM response: " + e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    String llmResponse = get();
                    if (llmResponse != null) {
                        displayLLMResponse(project, llmResponse);
                    }
                    feedbackPanel.setVisible(true);
                } catch (InterruptedException | ExecutionException e) {
                    LoggerUtil.error("Error retrieving LLM response in done(): " + e.getMessage(), e);
                    Thread.currentThread().interrupt();
                } finally {
                    loadingDialog.dispose();
                }
            }
        };
        worker.execute();
        loadingDialog.setVisible(true);
    }

    /**
     * Displays the LLM response in the text area and shows the diff view.
     * @param project The current IntelliJ project.
     * @param llmResponse The LLM response to display.
     */
    private void displayLLMResponse(Project project, String llmResponse) {
        try {
            Optional<VirtualFile> currentFileOpt = FileDetector.detectCurrentJavaFile(project);
            if (currentFileOpt.isPresent()) {
                VirtualFile currentFile = currentFileOpt.get();
                Document document = FileDocumentManager.getInstance().getDocument(currentFile);
                if (document != null) {
                    // Retrieve the last code snippets from RequestStorage
                    String extractedSnippet = String.join("\n", RequestStorage.getCodeSnippets());
                    // Pass the extracted snippet to the openDiffView method
                    openDiffView(project, currentFile, extractedSnippet, llmResponse);
                }
            }
            llmResponseTextArea.setText(llmResponse);
            llmResponseTextArea.setCaretPosition(0);
        } catch (Exception e) {
            LoggerUtil.error("Error displaying LLM response: " + e.getMessage(), e);
        }
    }

    /**
     * Opens a diff view to compare the original code with the LLM refactored code.
     * @param project The current IntelliJ project.
     * @param currentFile The current file being analyzed.
     * @param extractedSnippet The original document text.
     * @param llmResponse The LLM refactored code.
     */
    private void openDiffView(Project project, VirtualFile currentFile, String extractedSnippet, String llmResponse) {
        try {
            Document llmDocument = EditorFactory.getInstance().createDocument(llmResponse);

            DocumentContent originalContent =
                    DiffContentFactory.getInstance().create(project, extractedSnippet, currentFile.getFileType());
            DocumentContent modifiedContent =
                    DiffContentFactory.getInstance().create(project, llmDocument, currentFile.getFileType());

            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "Code Refactoring - " + currentFile.getName(),
                    originalContent,
                    modifiedContent,
                    "Original code snippet",
                    "LLM suggestions"
            );
            DiffManager.getInstance().showDiff(project, diffRequest);
        } catch (Exception e) {
            LoggerUtil.error("Error opening diff view: " + e.getMessage(), e);
        }
    }

    /**
     * Creates and configures the loading dialog.
     *
     * @return Configured JDialog for loading indication.
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
}