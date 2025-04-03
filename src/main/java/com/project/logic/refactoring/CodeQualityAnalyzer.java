package com.project.logic.refactoring;

import com.intellij.openapi.vfs.VirtualFile;
import com.project.logic.PMDRunner;
import com.project.util.LoggerUtil;

/**
 * Handles code quality analysis operations using PMD.
 *
 * @author Sara Moussa
 */
public class CodeQualityAnalyzer {

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

            // Run PMD analysis on the original and processed content
            PMDRunner pmdRunner = new PMDRunner();
            String originalPmdResults = pmdRunner.runPMD(currentFile.getPath());
            String processedPmdResults = pmdRunner.runPMDOnString(processedContent, currentFile.getName());

            LoggerUtil.info("PMD Analysis Results for original file:");
            LoggerUtil.info(originalPmdResults);
            LoggerUtil.info("PMD Analysis Results for processed file:");
            LoggerUtil.info(processedPmdResults);

            compareAndCreateResult(originalPmdResults, processedPmdResults);
        } catch (Exception e) {
            LoggerUtil.error("Error analyzing code quality: " + e.getMessage(), e);
        }
    }

    /**
     * Compares PMD analysis results and creates an AnalysisResult object.
     *
     * @param originalResults  PMD results from the original file
     * @param processedResults PMD results from the processed file
     */
    private void compareAndCreateResult(String originalResults, String processedResults) {
        int originalViolationCount = countViolations(originalResults);
        int processedViolationCount = countViolations(processedResults);

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

    }

    /**
     * Counts the number of PMD violations in the analysis results.
     *
     * @param pmdResults The PMD results text
     * @return The number of violations found
     */
    private int countViolations(String pmdResults) {
        if (pmdResults == null || pmdResults.trim().isEmpty()) {
            return 0;
        }

        int count = 0;
        String[] lines = pmdResults.split("\n");
        for (String line : lines) {
            if (line.contains("Problem") || (line.contains(".java:") && !line.contains("No violations found"))) {
                count++;
            }
        }
        return count;
    }
}