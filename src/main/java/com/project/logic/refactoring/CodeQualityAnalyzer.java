package com.project.logic.refactoring;

import com.github.javaparser.JavaParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.logic.parsing.CodeParser;
import com.project.logic.analysis.PMDRunner;
import com.project.logic.analysis.ViolationExtractor;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.ui.core.CodeQualityResultDialog;
import com.project.util.LoggerUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
     * Analyzes the code quality using PMD for both original and processed content at three levels.
     *
     * @param currentFile      The original file
     * @param processedContent The processed content with LLM changes
     * @param llmRawContent    The raw content from LLM before any processing
     */
    public void analyzeCodeQuality(VirtualFile currentFile, String processedContent, String llmRawContent) {
        try {
            if (processedContent == null) {
                LoggerUtil.info("No processed content available for PMD analysis");
                return;
            }

            // Run PMD analysis on the original file with normal rules
            PMDRunner originalPmdRunner = new PMDRunner();
            String originalPmdOutput = originalPmdRunner.runPMD(currentFile.getPath());
            List<Violation> originalViolations = violationExtractor.extractViolations(originalPmdOutput);
            List<CodeBlockInfo> originalBlocks = codeParser.extractViolatedBlocksInfo(currentFile.getPath(), originalViolations);
            String originalFormattedResults = responseFormatter.formatUserResponse(originalBlocks);

            // Run PMD analysis on the processed content with normal rules
            PMDRunner processedPmdRunner = new PMDRunner();
            String processedPmdOutput = processedPmdRunner.runPMDOnString(processedContent, currentFile.getName());
            List<Violation> processedViolations = violationExtractor.extractViolations(processedPmdOutput);

            // Create temporary file for processed content
            String tempProcessedFileName = "processed_" + currentFile.getName();
            Path tempDir = Files.createTempDirectory("refactoring_");
            Path tempProcessedFilePath = tempDir.resolve(tempProcessedFileName);
            Files.writeString(tempProcessedFilePath, processedContent);

            List<CodeBlockInfo> processedBlocks = codeParser.extractViolatedBlocksInfo(
                    tempProcessedFilePath.toString(), processedViolations);
            String processedFormattedResults = responseFormatter.formatUserResponse(processedBlocks);

            // Run PMD analysis on LLM raw content with temporary ruleset
            Path tempRulesetPath = createTemporaryRuleset();
            PMDRunner llmRawPmdRunner = new PMDRunner();
            llmRawPmdRunner.setTemporaryRuleSet(tempRulesetPath.toString());

            String llmRawPmdOutput = llmRawPmdRunner.runPMDOnString(llmRawContent, currentFile.getName());
            List<Violation> llmRawViolations = violationExtractor.extractViolations(llmRawPmdOutput);

            // Create temporary file for LLM raw content
            String tempLlmRawFileName = "llm_raw_" + currentFile.getName();
            Path tempLlmRawFilePath = tempDir.resolve(tempLlmRawFileName);
            Files.writeString(tempLlmRawFilePath, llmRawContent);

            List<CodeBlockInfo> llmRawBlocks = codeParser.extractViolatedBlocksInfo(
                    tempLlmRawFilePath.toString(), llmRawViolations);
            String llmRawFormattedResults = responseFormatter.formatUserResponse(llmRawBlocks);

            // Create summary of metrics
            Map<String, String> metricsSummary = MetricsExtractor.calculateMetrics(llmRawFormattedResults);


            // Clean up temporary files
            Files.deleteIfExists(tempProcessedFilePath);
            Files.deleteIfExists(tempLlmRawFilePath);
            Files.deleteIfExists(tempRulesetPath);
            Files.deleteIfExists(tempDir);

            // Log the formatted results
            LoggerUtil.info("PMD Analysis Results for original file (normal rules):");
            LoggerUtil.info(originalFormattedResults);
            LoggerUtil.info("PMD Analysis Results for processed file (normal rules):");
            LoggerUtil.info(processedFormattedResults);
            LoggerUtil.info("PMD Analysis Results for LLM raw file (stricter rules):");
            LoggerUtil.info(llmRawFormattedResults);
            LoggerUtil.info("Metrics Summary: " + metricsSummary);

            // Compare and show results
            compareAndCreateResult(
                    originalFormattedResults,
                    processedFormattedResults,
                    llmRawFormattedResults,
                    originalViolations.size(),
                    processedViolations.size(),
                    llmRawViolations.size(),
                    currentFile.getName(),
                    metricsSummary
            );
        } catch (Exception e) {
            LoggerUtil.error("Error analyzing code quality: " + e.getMessage(), e);
        }
    }

    public Path createTemporaryRuleset() throws IOException {
        String rulesetContent = """
        <ruleset name="Temporary Ruleset"
                 xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd">
            <description>Temporary ruleset with lower thresholds.</description>

            <rule ref="category/java/design.xml/CyclomaticComplexity">
                <properties>
                    <property name="methodReportLevel" value="1"/>
                </properties>
            </rule>

            <rule ref="category/java/design.xml/CognitiveComplexity">
                <properties>
                    <property name="reportLevel" value="1"/>
                </properties>
            </rule>

            <rule ref="category/java/design.xml/NPathComplexity">
                <properties>
                    <property name="reportLevel" value="1"/>
                </properties>
            </rule>

            <rule name="ExcessiveLinesOfCode"
                  language="java"
                  message="Method has too many lines of code"
                  class="com.project.logic.analysis.ExcessiveLinesOfCodeRule">
                <properties>
                    <property name="threshold" value="1"/>
                </properties>
            </rule>
        </ruleset>
        """;

        Path tempRulesetPath = Files.createTempFile("pmd_temp_ruleset", ".xml");
        Files.writeString(tempRulesetPath, rulesetContent);
        return tempRulesetPath;
    }

    /**
     * Compares PMD analysis results and creates an AnalysisResult object.
     * Shows a dialog with the analysis results.
     *
     * @param originalResults        Formatted PMD results from the original file
     * @param processedResults       Formatted PMD results from the processed file
     * @param llmRawResults          Formatted PMD results from the LLM raw file
     * @param originalViolationCount Number of violations in the original file
     * @param processedViolationCount Number of violations in the processed file
     * @param llmRawViolationCount    Number of violations in the LLM raw file with stricter rules
     * @param fileName               Name of the file being analyzed
     */
    private void compareAndCreateResult(
            String originalResults,
            String processedResults,
            String llmRawResults,
            int originalViolationCount,
            int processedViolationCount,
            int llmRawViolationCount,
            String fileName,
            Map<String, String> metricsSummary) {

        LoggerUtil.info("Original PMD violations (normal rules): " + originalViolationCount);
        LoggerUtil.info("Processed PMD violations (normal rules): " + processedViolationCount);
        LoggerUtil.info("LLM raw PMD violations (stricter rules): " + llmRawViolationCount);

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
            new CodeQualityResultDialog(project, fileName,
                    originalResults, processedResults, llmRawResults,
                    originalViolationCount, processedViolationCount, llmRawViolationCount,
                    message, metricsSummary).show();
        });
    }
}