package com.project.logic.refactoring;

import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats code block extraction results into:
 * A structured JSON response for API consumption.
 * A user-friendly summary message.
 *
 * @author Sara Moussa.
 */
public class ResponseFormatter {

    /**
     * Formats the extracted code blocks into a structured JSON response for API consumption.
     *
     * @param blocksInfo The list of extracted CodeBlockInfo objects.
     * @return A formatted JSON string representing the API payload.
     */
    public String formatApiResponse(List<CodeBlockInfo> blocksInfo, boolean includeIssuesAndObjectives) {
        if (blocksInfo.isEmpty()) return "[]";

        // Get file name
        String fileName = getFileName(blocksInfo.get(0).filePath());

        // Extract class signatures
        List<ClassInfo> classInfos = blocksInfo.stream()
                .filter(block -> "Class".equals(block.blockType()))
                .map(block -> new ClassInfo(block.codeSnippet(), block.startLine(), block.endLine()))
                .collect(Collectors.toList());

        // Group violations under their class
        Map<ClassInfo, List<CodeBlockInfo>> violationsByClass = blocksInfo.stream()
                .filter(block -> !"Class".equals(block.blockType()))
                .collect(Collectors.groupingBy(block -> findContainingClass(block, classInfos)));

        // Build JSON response
        StringBuilder response = new StringBuilder("{\n");
        response.append("  \"instruction\": \"")
                .append(escapeJson(RefactoringObjectiveProvider.getLLMInstruction())).append("\",\n");
        response.append("  \"file\": \"").append(escapeJson(fileName)).append("\",\n");
        response.append("  \"classes\": [\n");

        boolean firstClass = true;
        for (ClassInfo classInfo : classInfos) {
            if (!firstClass) response.append(",\n");
            firstClass = false;

            response.append("    {\n");
            response.append("      \"type_signature\": \"").append(escapeJson(classInfo.signature())).append("\",\n");
            response.append("      \"violations\": [\n");

            List<CodeBlockInfo> violations = violationsByClass.getOrDefault(classInfo, List.of());
            String violationsJson = violations.stream()
                    .map(info -> formatGroupedEntry(info, includeIssuesAndObjectives))
                    .collect(Collectors.joining(",\n"));

            response.append(violationsJson);
            response.append("\n      ]\n");
            response.append("    }");
        }

        response.append("\n  ]\n}");

        LoggerUtil.info("Generated API JSON response for file: " + fileName);
        return response.toString();
    }

    /**
     * Finds the correct class for a given violation block based on its start line.
     *
     * @param block The violation block.
     * @param classInfos The list of detected classes.
     * @return The class that contains the given block.
     */
    private ClassInfo findContainingClass(CodeBlockInfo block, List<ClassInfo> classInfos) {
        return classInfos.stream()
                .filter(classInfo -> block.startLine() >= classInfo.startLine() && block.startLine() <= classInfo.endLine())
                .findFirst()
                .orElse(new ClassInfo("Unknown", -1, -1));
    }

    /**
     * Class to store class signature and its range.
     */
    private record ClassInfo(String signature, int startLine, int endLine) {}

    /**
     * Formats extracted violations into a user-friendly summary message.
     *
     * @param blocksInfo The list of extracted CodeBlockInfo objects.
     * @return A human-readable summary of the PMD analysis results.
     */
    public String formatUserResponse(List<CodeBlockInfo> blocksInfo) {
        if (blocksInfo.isEmpty()) {
            return "No violations detected in the analyzed file.";
        }

        StringBuilder response = new StringBuilder();
        String fileName = getFileName(blocksInfo.get(0).filePath());
        response.append("PMD Analysis Summary for file: ").append(fileName).append("\n");

        // Extract all top-level type declarations (classes, interfaces, etc.)
        List<ClassInfo> typeInfos = blocksInfo.stream()
                .filter(b -> "Class".equals(b.blockType()))
                .map(b -> new ClassInfo(b.codeSnippet(), b.startLine(), b.endLine()))
                .sorted(Comparator.comparingInt(ClassInfo::startLine))
                .toList();


        // Gather all violation blocks
        List<CodeBlockInfo> violationBlocks = blocksInfo.stream()
                .filter(b -> !"Class".equals(b.blockType()))
                .toList();

        // Process each type separately
        for (ClassInfo typeInfo : typeInfos) {
            response.append("\nType Signature: ").append(typeInfo.signature())
                    .append(" (Lines ").append(typeInfo.startLine())
                    .append("-").append(typeInfo.endLine()).append(")\n");

            // Filter violations that belong to this type
            List<CodeBlockInfo> typeViolations = violationBlocks.stream()
                    .filter(v -> v.startLine() >= typeInfo.startLine() && v.endLine() <= typeInfo.endLine())
                    .toList();

            if (typeViolations.isEmpty()) {
                response.append("  No violations detected in this type.\n");
            } else {
                for (CodeBlockInfo violationBlock : typeViolations) {
                    response.append("\n  ").append(violationBlock.blockType())
                            .append(" (Lines ").append(violationBlock.startLine())
                            .append("-").append(violationBlock.endLine()).append(")\n");

                    for (Violation violation : violationBlock.violations()) {
                        response.append("    - ").append(violation.ruleName()).append(": ")
                                .append(violation.message()).append("\n");
                    }
                }
            }
        }

        LoggerUtil.info("Generated structured user-friendly PMD analysis summary for file: " + fileName);
        return response.toString();
    }

    /**
     * Formats an individual extracted code block for the JSON response.
     *
     * @param info The extracted code block information.
     * @param includeIssues Whether to include issues and refactoring objectives in the output.
     * @return A formatted JSON entry for the extracted block.
     */
    private String formatGroupedEntry(CodeBlockInfo info, boolean includeIssues) {
        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"block_type\": \"").append(escapeJson(info.blockType())).append("\",\n");
        sb.append("      \"extracted_code\": \"").append(escapeJson(info.codeSnippet())).append("\",\n");

        // include issues and refactoring objectives
        if (includeIssues) {
            sb.append(formatIssuesAndObjective(info));
        } else {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("    }");

        return sb.toString();
    }

    /**
     * Creates the JSON content for violations and refactoring objectives.
     * @param info The code block information containing violation details.
     * @return A formatted JSON string with issues and refactoring objectives.
     */
    private String formatIssuesAndObjective(CodeBlockInfo info) {
        StringBuilder sb = new StringBuilder();

        // Add issues array
        sb.append("      \"issues\": [\n");

        // Extract all rule names to generate a combined refactoring objective
        Set<String> ruleNames = info.violations().stream()
                .map(Violation::ruleName)
                .collect(Collectors.toSet());

        String violationsJson = info.violations().stream()
                .map(violation -> String.format("        {\n          \"rule\": \"%s\",\n          \"message\": \"%s\"\n        }",
                        escapeJson(violation.ruleName()),
                        escapeJson(violation.message())))
                .collect(Collectors.joining(",\n"));

        sb.append(violationsJson);
        sb.append("\n      ],\n");

        // Add refactoring objective based on detected violations
        sb.append("      \"refactoring_objective\": \"")
                .append(escapeJson(RefactoringObjectiveProvider.getRefactoringObjective(ruleNames)))
                .append("\"\n");

        return sb.toString();
    }

    /**
     * Escapes JSON special characters in a string.
     *
     * @param input The input string.
     * @return The escaped string.
     */
    private String escapeJson(String input) {
        return (input == null) ? "" : input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Extracts the file name from a given file path.
     *
     * @param filePath The full path of the file.
     * @return The name of the file without the directory path.
     *
     * @throws IllegalArgumentException If the provided file path is null or empty.
     */
    private String getFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }
        return Paths.get(filePath).getFileName().toString();
    }
}
