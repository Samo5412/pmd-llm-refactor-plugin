package com.project.logic.refactoring;

import com.github.javaparser.JavaParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.logic.CodeParser;
import com.project.logic.PMDRunner;
import com.project.logic.ResponseFormatter;
import com.project.logic.ViolationExtractor;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.ui.CodeQualityResultDialog;
import com.project.util.LoggerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Handles code quality analysis operations using PMD.
 *
 * @author Sara Moussa
 */
public class CodeQualityAnalyzer {

    private final ResponseFormatter responseFormatter = new ResponseFormatter();
    private final ViolationExtractor violationExtractor = new ViolationExtractor();
    private final CodeParser codeParser = new CodeParser(new JavaParser());

    /**
     * Analyzes the code quality using PMD for both original and processed content.
     *
     * @param currentFile      The original file
     * @param processedContent The processed content with LLM changes
     */
    public void analyzeCodeQuality(VirtualFile currentFile, String processedContent) {
        try {
            if (processedContent == null) {
                LoggerUtil.info("No processed content available for PMD analysis");
                return;
            }

            // Run PMD analysis on the original file
            PMDRunner pmdRunner = new PMDRunner();
            String originalPmdOutput = pmdRunner.runPMD(currentFile.getPath());
            List<Violation> originalViolations = violationExtractor.extractViolations(originalPmdOutput);
            List<CodeBlockInfo> originalBlocks = codeParser.extractViolatedBlocksInfo(currentFile.getPath(), originalViolations);
            String originalFormattedResults = responseFormatter.formatUserResponse(originalBlocks);

            // Run PMD analysis on the processed content
            String processedPmdOutput = pmdRunner.runPMDOnString(processedContent, currentFile.getName());
            List<Violation> processedViolations = violationExtractor.extractViolations(processedPmdOutput);
            String tempFileName = "processed_" + currentFile.getName();
            Path tempDir = Files.createTempDirectory("refactoring_");
            Path tempFilePath = tempDir.resolve(tempFileName);
            Files.writeString(tempFilePath, processedContent);

            List<CodeBlockInfo> processedBlocks = codeParser.extractViolatedBlocksInfo(
                    tempFilePath.toString(), processedViolations);


            Files.delete(tempFilePath);

            String processedFormattedResults = responseFormatter.formatUserResponse(processedBlocks);

            // Log the formatted results
            LoggerUtil.info("PMD Analysis Results for original file:");
            LoggerUtil.info(originalFormattedResults);
            LoggerUtil.info("PMD Analysis Results for processed file:");
            LoggerUtil.info(processedFormattedResults);

            // Compare and show results
            compareAndCreateResult(
                    originalFormattedResults,
                    processedFormattedResults,
                    originalViolations.size(),
                    processedViolations.size(),
                    currentFile.getName()
            );
        } catch (Exception e) {
            LoggerUtil.error("Error analyzing code quality: " + e.getMessage(), e);
        }
    }


    /**
     * Compares PMD analysis results and creates an AnalysisResult object.
     * Shows a dialog with the analysis results.
     *
     * @param originalResults        Formatted PMD results from the original file
     * @param processedResults       Formatted PMD results from the processed file
     * @param originalViolationCount Number of violations in the original file
     * @param processedViolationCount Number of violations in the processed file
     * @param fileName               Name of the file being analyzed
     */
    private void compareAndCreateResult(
            String originalResults,
            String processedResults,
            int originalViolationCount,
            int processedViolationCount,
            String fileName) {

        LoggerUtil.info("Original PMD violations: " + originalViolationCount);
        LoggerUtil.info("Processed PMD violations: " + processedViolationCount);

        boolean issuesFixed = processedViolationCount < originalViolationCount;
        String message;
        if (issuesFixed) {
            message = "LLM response fixed " + (originalViolationCount - processedViolationCount) + " PMD issues!";
            LoggerUtil.info(message);
        } else if (processedViolationCount == originalViolationCount) {
            message = "LLM response didn't affect PMD issues.";
            LoggerUtil.info(message);
        } else {
            message = "LLM response introduced " + (processedViolationCount - originalViolationCount) + " new PMD issues!";
            LoggerUtil.warn(message);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Project project = ProjectManager.getInstance().getOpenProjects()[0];
            new CodeQualityResultDialog(project, fileName, originalResults, processedResults,
                    originalViolationCount, processedViolationCount, message).show();
        });
    }
}