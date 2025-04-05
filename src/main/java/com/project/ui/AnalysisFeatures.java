package com.project.ui;

import com.github.javaparser.JavaParser;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.api.LLMService;
import com.project.api.RequestStorage;
import com.project.logic.*;
import com.project.logic.refactoring.CodeQualityAnalyzer;
import com.project.logic.refactoring.InMemoryFileStorage;
import com.project.logic.refactoring.MarkerBlockProcessor;
import com.project.model.BatchPreparationResult;
import com.project.settings.SettingsManager;
import com.project.ui.util.FileAnalysisTracker;
import com.project.util.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    /**
     * Processor for handling marker blocks in code.
     */
    private final MarkerBlockProcessor markerBlockProcessor;

    /**
     * Constructor to initialize the analysis features.
     *
     * @param resultTextArea        Text area for displaying PMD analysis results.
     * @param llmResponseTextArea   Text area for displaying LLM responses.
     * @param fileAnalysisTracker    Tracker for caching file analysis results.
     * @param pmdButton             Button to trigger PMD analysis or LLM response.
     * @param statusLabel           Label to display the status of the analysis.
     * @param feedbackPanel         Panel for displaying user feedback options.
     */
    public AnalysisFeatures(JTextArea resultTextArea, JTextArea llmResponseTextArea, FileAnalysisTracker fileAnalysisTracker, JButton pmdButton, JLabel statusLabel, JPanel feedbackPanel) {
        this.resultTextArea = resultTextArea;
        this.llmResponseTextArea = llmResponseTextArea;
        this.fileAnalysisTracker = fileAnalysisTracker;
        this.pmdButton = pmdButton;
        this.statusLabel = statusLabel;
        this.feedbackPanel = feedbackPanel;
        this.markerBlockProcessor = new MarkerBlockProcessor();
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
                storeOriginalFileIfViolated(file, filePath, codeParser);
            } else {
                resetToAnalyzeMode(project);
            }
        });
    }

    /**
     * Stores a version of the specified file in memory after removing flagged code blocks.
     *
     * @param file       The virtual file being processed.
     * @param filePath   The path to the original file on the filesystem.
     * @param codeParser Utility to parse the file and remove flagged code blocks.
     */
    private void storeOriginalFileIfViolated(VirtualFile file, String filePath, CodeParser codeParser) {
        try {
            String originalContent = Files.readString(Path.of(filePath));

            String markedContent = codeParser.insertMarkers(
                    originalContent,
                    lastBatchResult != null ? lastBatchResult.allBlocks() : Collections.emptyList()
            );

            InMemoryFileStorage storage = ApplicationManager.getApplication().getService(InMemoryFileStorage.class);
            storage.storeOriginalVersion(file, markedContent);
            LoggerUtil.info("Marked version of file stored in memory:" + markedContent);
        } catch (Exception e) {
            LoggerUtil.error("Failed to store marked version: " + e.getMessage(), e);
        }
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
                    long startTime = System.currentTimeMillis();

                    String prompt = generatePromptFromBatchResult();

                    String model = SettingsManager.getInstance().getModelName();
                    int maxTokens = Integer.parseInt(SettingsManager.getInstance().getTokenAmount());
                    double temperature = Double.parseDouble(SettingsManager.getInstance().getTemperature());

                    String llmResponse = LLMService.getLLMResponse(prompt, model, maxTokens, temperature);

                    // End timing here and log
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    LoggerUtil.info("LLM processing took: " + elapsedTime + " ms");

                    fileAnalysisTracker.cacheLLMResponse(filePath, llmResponse);

                    return llmResponse;

                } catch (Exception e) {
                    LoggerUtil.error("Error calling LLM service: " + e.getMessage(), e);
                    return "An error occurred while fetching the LLM response: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String llmResponse = get();
                    if (llmResponse != null) {
                        displayLLMResponse(project, llmResponse);
                        updateButtonToViewDiff(project);
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
     * Generates a prompt based on the results of the last batch processing.
     *
     * @return The generated prompt string or a default prompt if no batch result is available.
     */
    private String generatePromptFromBatchResult() {
        if (lastBatchResult != null && !lastBatchResult.batches().isEmpty()) {
            ResponseFormatter formatter = new ResponseFormatter();
            return formatter.formatApiResponse(lastBatchResult.batches().get(0));
        }
        return "Refactor the following Java code according to PMD suggestions.";
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
                    // Process the response and get the processed content
                    String processedContent = processLLMResponse(llmResponse, currentFile);

                    // Analyze the code quality with PMD
                    CodeQualityAnalyzer analyzer = new CodeQualityAnalyzer();
                    analyzer.analyzeCodeQuality(currentFile, processedContent);
                    LoggerUtil.info("Processed content: " + processedContent);

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
     * Processes the LLM response and updates the file content.
     *
     * @param llmResponse The LLM response to process.
     * @param currentFile The current file being analyzed.
     * @return The processed content or null if an error occurs.
     */
    private String processLLMResponse(String llmResponse, VirtualFile currentFile) {
        try {
            // Process the LLM response with MarkerBlockProcessor
            Map<String, String> methodMap = markerBlockProcessor.parseMethodsFromLLMResponse(llmResponse);
            LoggerUtil.info("Parsed " + methodMap.size() + " methods from LLM response");

            // Get the stored marked version of the file
            InMemoryFileStorage storage = ApplicationManager.getApplication().getService(InMemoryFileStorage.class);
            String markedContent = storage.getOriginalVersion(currentFile);

            if (markedContent != null) {
                String processedContent = markerBlockProcessor.processMarkedFile(markedContent, methodMap);
                LoggerUtil.info("Processed file with marker replacements");

                // Store the processed content for future use
                storage.storeProcessedVersion(currentFile, processedContent);
                LoggerUtil.info("Stored processed version of file: " + currentFile.getName());

                return processedContent;
            }
            return null;
        } catch (Exception e) {
            LoggerUtil.error("Error processing LLM response: " + e.getMessage(), e);
            return null;
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

    /**
     * Updates the PMD button to clearly show cached LLM response (diff mode).
     * @param project The current IntelliJ project.
     */
    public void updateButtonToViewDiff(Project project) {
        pmdButton.setText("View Diff");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> handleViewDiff(project));
    }

    /**
     * Handles the action of viewing the diff between the original and LLM refactored code.
     * @param project The current IntelliJ project.
     */
    private void handleViewDiff(Project project) {
        Optional<VirtualFile> currentFileOpt = FileDetector.detectCurrentJavaFile(project);
        if (currentFileOpt.isPresent()) {
            VirtualFile file = currentFileOpt.get();
            String cachedResponse = fileAnalysisTracker.getCachedLLMResponse(file.getPath());

            if (cachedResponse != null) {
                displayLLMResponse(project, cachedResponse);
                statusLabel.setText("Displaying cached LLM response.");
            } else {
                resultTextArea.setText("No cached response available. Please generate a new one.");
                statusLabel.setText("Please generate a fresh LLM response.");
                resetToLLMGenerationMode(project);
            }
        } else {
            resultTextArea.setText("Error: No Java file selected.");
            statusLabel.setText("Java file selection error.");
        }
    }

    /**
     * Resets the state clearly to LLM Generation mode.
     * This is used to allow explicitly requesting a fresh LLM response.
     *
     * @param project The current IntelliJ project.
     */
    public void resetToLLMGenerationMode(Project project) {
        pmdButton.setText("Get LLM Response");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> showLLMProcessingDialog(project));
        statusLabel.setText("Ready to generate a new LLM response.");
    }

    /**
     * Explicit method clearly defining handling regeneration of fresh LLM response.
     * @param project The IntelliJ project context.
     */
    public void regenerateLLMResponse(Project project) {
        Optional<VirtualFile> currentFileOpt = FileDetector.detectCurrentJavaFile(project);
        if (currentFileOpt.isEmpty()) {
            statusLabel.setText("No Java file selected.");
            return;
        }

        VirtualFile file = currentFileOpt.get();
        String filePath = file.getPath();

        fileAnalysisTracker.invalidateLLMResponse(filePath);

        showLLMProcessingDialog(project);

        statusLabel.setText("Regenerating fresh LLM response...");
    }
}