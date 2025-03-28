package com.project.ui;

import com.github.javaparser.JavaParser;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.project.logic.*;
import com.project.model.BatchPreparationResult;
import com.project.util.LoggerUtil;
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
     * The batch preparation result from the last analysis.
     */
    private BatchPreparationResult lastBatchResult;

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

        JPanel southPanel = createSouthPanel(project);
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
     * @param project The current IntelliJ project.
     * @return The initialized JPanel for the south section.
     */
    private JPanel createSouthPanel(Project project) {
        pmdButton = new JButton("Analyse Code");
        pmdButton.setEnabled(false);  // Initially disabled
        pmdButton.addActionListener(e -> runPMDAnalysis(project));

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
     * Shows a dialog while processing LLM requests.
     *
     * @param project The current IntelliJ project
     */
    private void showLLMProcessingDialog(Project project) {
        JDialog loadingDialog = createLoadingDialog();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String llmResponse = "public class HelloWorld {\n" + // TODO: Replace with actual LLM response
                            "    public static void main(String[] args) {\n" +
                            "        System.out.println(\"Hello, World!\");\n" +
                            "    }\n" +
                            "}";
                    LoggerUtil.info("LLM response received: " + llmResponse);

                    SwingUtilities.invokeLater(() -> displayLLMResponse(project, llmResponse));
                } catch (Exception e) {
                    LoggerUtil.error("Error processing LLM response: " + e.getMessage(), e);
                }
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                showBatchResultMessage();
                feedbackPanel.setVisible(true);
            }
        };
        LoggerUtil.info("Executing SwingWorker");
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
                    openDiffView(project, currentFile, document.getText(), llmResponse);
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
     * @param orgDocument The original document text.
     * @param llmResponse The LLM refactored code.
     */
    private void openDiffView(Project project, VirtualFile currentFile, String orgDocument, String llmResponse) {
        try {
            Document llmDocument = EditorFactory.getInstance().createDocument(llmResponse);

            PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
            FileType fileType = psiFile != null ? psiFile.getFileType() : currentFile.getFileType();

            DocumentContent originalContent =
                    DiffContentFactory.getInstance().create(project, orgDocument, fileType);
            DocumentContent modifiedContent =
                    DiffContentFactory.getInstance().create(project, llmDocument, fileType);

            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                    "Code Refactoring - Before and After",
                    originalContent,
                    modifiedContent,
                    "Original code",
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

    /**
     * Updates the PMD button to handle LLM response requests.
     *
     * @param project The current IntelliJ project
     */
    private void updateButtonForLLMResponse(Project project) {
        pmdButton.setText("Get LLM Response");
        for (var listener : pmdButton.getActionListeners()) {
            pmdButton.removeActionListener(listener);
        }
        pmdButton.addActionListener(e -> showLLMProcessingDialog(project));
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
                feedbackPanel.setVisible(false);
            }
        } else {
            // File not analyzed yet, show analyze button
            setFileStatus("Java file detected: " + file.getName(), true);
            resetToAnalyzeMode(project);
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