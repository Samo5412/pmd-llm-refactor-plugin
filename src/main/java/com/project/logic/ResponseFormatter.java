package com.project.logic;

import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.util.List;
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
    public String formatApiResponse(List<CodeBlockInfo> blocksInfo) {
        StringBuilder response = new StringBuilder("{\n  \"PMD Analysis Results\": [\n");

        List<String> groupedResults = blocksInfo.stream()
                .map(this::formatGroupedEntry)
                .collect(Collectors.toList());

        response.append(String.join(",\n", groupedResults));
        response.append("\n  ]\n}");

        LoggerUtil.info("Generated API JSON response with " + blocksInfo.size() + " code blocks.");
        return response.toString();
    }

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
        response.append("PMD Analysis Summary:\n");

        for (CodeBlockInfo info : blocksInfo) {
            response.append("\nFile: ").append(info.filePath());
            response.append("\nBlock Type: ").append(info.blockType())
                    .append(" (Lines: ").append(info.startLine()).append("-").append(info.endLine()).append(")");

            response.append("\nViolations:");
            for (Violation violation : info.violations()) {
                response.append("\n  - Line ").append(violation.lineNumber()).append(": ")
                        .append(violation.ruleName()).append(" - ")
                        .append(violation.message());
            }
            response.append("\n");
        }

        LoggerUtil.info("Generated user-friendly response with " + blocksInfo.size() + " violations.");
        return response.toString();
    }

    /**
     * Formats an individual extracted code block for the JSON response.
     *
     * @param info The extracted code block information.
     * @return A formatted JSON entry for the extracted block.
     */
    private String formatGroupedEntry(CodeBlockInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"file\": \"").append(escapeJson(info.filePath())).append("\",\n");
        sb.append("      \"block_type\": \"").append(escapeJson(info.blockType())).append("\",\n");
        sb.append("      \"start_line\": ").append(info.startLine()).append(",\n");
        sb.append("      \"end_line\": ").append(info.endLine()).append(",\n");
        sb.append("      \"extracted_code\": \"").append(escapeJson(info.codeSnippet())).append("\",\n");
        sb.append("      \"violations\": [\n");

        String violationsJson = info.violations().stream()
                .map(violation -> String.format("        {\n          \"line\": %d,\n          \"rule\": \"%s\",\n          \"message\": \"%s\"\n        }",
                        violation.lineNumber(),
                        escapeJson(violation.ruleName()),
                        escapeJson(violation.message())))
                .collect(Collectors.joining(",\n"));

        sb.append(violationsJson);
        sb.append("\n      ]\n    }");

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
}
