package com.project.logic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.model.BatchPreparationResult;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.util.List;

/**
 * Handles the complete process of PMD analysis, violation extraction, and response formatting.
 *
 * @author Micael Olsson
 */
public class PMDAnalyzer {

    /** Runs PMD analysis on Java files. */
    private final PMDRunner pmdRunner;

    /** Extracts violations from PMD output. */
    private final ViolationExtractor violationExtractor;

    /** Parses Java files to extract violated code blocks. */
    private final CodeParser codeParser;

    /** Formats analysis results into user-friendly and API responses. */
    private final ResponseFormatter responseFormatter;

    /**
     * Initializes the analyzer with required dependencies.
     *
     * @param pmdRunner          PMD execution utility.
     * @param violationExtractor Extracts violations from PMD output.
     * @param codeParser Extracts violated code blocks.
     * @param responseFormatter  Formats analysis results.
     */
    public PMDAnalyzer(PMDRunner pmdRunner, ViolationExtractor violationExtractor,
                       CodeParser codeParser, ResponseFormatter responseFormatter) {
        this.pmdRunner = pmdRunner;
        this.violationExtractor = violationExtractor;
        this.codeParser = codeParser;
        this.responseFormatter = responseFormatter;
    }

    /**
     * Runs the full PMD analysis on a Java file and returns a user-friendly response.
     *
     * @param project The IntelliJ project containing the file.
     * @param file    The Java file to analyze.
     * @return A user-friendly analysis result message.
     */
    public String analyzeFile(Project project, VirtualFile file) {
        validateProjectAndFile(project, file);
        String filePath = file.getPath();

        LoggerUtil.info("Starting PMD analysis for file: " + filePath);

        String pmdOutput = pmdRunner.runPMD(filePath);
        List<Violation> violations = violationExtractor.extractViolations(pmdOutput);

        if (violations.isEmpty()) {
            LoggerUtil.info("No issues found in file: " + filePath);
            return "No issues found.";
        }

        List<CodeBlockInfo> allBlocks = codeParser.extractViolatedBlocksInfo(filePath, violations);

        // Generate full analysis summary
        String userMessage = responseFormatter.formatUserResponse(allBlocks);

        // Prepare LLM-compatible batches
        BatchPreparationResult result = PromptBatchTrimmer.splitIntoBatches(allBlocks);

        LoggerUtil.info("Batching Summary: " + result.userMessage());

        List<List<CodeBlockInfo>> batches = result.batches();
        for (int i = 0; i < batches.size(); i++) {
            List<CodeBlockInfo> batch = batches.get(i);
            LoggerUtil.info("Prepared batch " + (i + 1) + " of " + batches.size());

            String jsonResponse = responseFormatter.formatApiResponse(batch);
            LoggerUtil.info("Generated API JSON response for batch " + (i + 1) + ":\n" + jsonResponse);
        }

        if (!result.skippedBlocks().isEmpty()) {
            LoggerUtil.warn("Skipped blocks due to size: " + result.skippedBlocks().size());
        }

        return userMessage;
    }

    /**
     * Validates the project and file input before proceeding with analysis.
     *
     * @param project The IntelliJ project.
     * @param file    The Java file to analyze.
     * @throws IllegalArgumentException if project or file is null.
     */
    private void validateProjectAndFile(Project project, VirtualFile file) {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null.");
        }
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
    }
}
