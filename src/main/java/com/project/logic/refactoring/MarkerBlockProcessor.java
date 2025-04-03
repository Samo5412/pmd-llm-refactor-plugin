package com.project.logic.refactoring;

import com.project.model.ResultWithUsedMethods;
import com.project.util.LoggerUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the replacement of code blocks between markers and manages extra methods
 * from LLM responses.
 *
 * @author Sara Moussa
 */
public class MarkerBlockProcessor {

    /**
     * Processes a file with marker pairs, replacing the marked code blocks with
     * corresponding implementations from a method map.
     *
     * @param markedFileContent The file content containing marker pairs
     * @param methodNameToCodeBlock Map of method names to their implementation code blocks
     * @return The file content with marker blocks replaced with implementations and extra methods appended
     */
    public String processMarkedFile(String markedFileContent, Map<String, String> methodNameToCodeBlock) {
        if (markedFileContent == null || markedFileContent.isEmpty()) {
            LoggerUtil.warn("Attempted to process null or empty file content");
            return markedFileContent;
        }

        ResultWithUsedMethods result = replaceMarkedBlocks(markedFileContent, methodNameToCodeBlock);

        return appendExtraMethods(result.content(), methodNameToCodeBlock, result.usedMethods());
    }

    /**
     * Replaces marked code blocks with corresponding implementations.
     *
     * @param markedFileContent The file content containing marker pairs
     * @param methodNameToCodeBlock Map of method names to their implementation code blocks
     * @return A result containing the modified content and a set of used method names
     */
    private ResultWithUsedMethods replaceMarkedBlocks(String markedFileContent, Map<String, String> methodNameToCodeBlock) {
        StringBuilder processedContent = new StringBuilder(markedFileContent);
        Set<String> usedMethods = new HashSet<>();

        // Pattern to match marker pairs
        Pattern markerPattern = Pattern.compile(
                "// <start-flagged:([^>]+)>([\\s\\S]*?)// <end-flagged:\\1>",
                Pattern.MULTILINE
        );

        Matcher matcher = markerPattern.matcher(markedFileContent);

        // Track offsets as we modify the string
        int offset = 0;

        while (matcher.find()) {
            String markerId = matcher.group(1);
            String markedBlock = matcher.group(2);

            // Extract method name from the marked block
            String methodName = extractMethodName(markedBlock);

            // Calculate positions with offset adjustment
            int startPos = matcher.start() + offset;
            int endPos = matcher.end() + offset;

            String replacement;

            if (methodName != null && methodNameToCodeBlock.containsKey(methodName)) {
                String replacementCode = methodNameToCodeBlock.get(methodName);
                usedMethods.add(methodName);

                // Prepare replacement with updated markers
                replacement = "// <start-replaced:" + markerId + ">\n" +
                        replacementCode + "\n" +
                        "// <end-replaced:" + markerId + ">";

                LoggerUtil.debug("Replaced marked block " + markerId +
                        " containing method '" + methodName + "' with implementation");
            } else {
                // Mark as removed if no matching replacement found
                replacement = "// <removed-method: " + methodName + " from marker " + markerId + ">\n" +
                        "// Original method was removed as no matching refactored implementation was found";

                LoggerUtil.warn("No replacement found for method '" + methodName +
                        "' in marker block " + markerId + ". Method removed.");
            }

            // Replace the entire marker block with either replacement or removal marker
            processedContent.replace(startPos, endPos, replacement);

            // Update offset
            int originalLength = matcher.end() - matcher.start();
            int newLength = replacement.length();
            offset += (newLength - originalLength);
        }

        return new ResultWithUsedMethods(processedContent.toString(), usedMethods);
    }

    /**
     * Appends any extra methods that weren't used to replace marked blocks.
     *
     * @param content The content after marked blocks have been processed
     * @param methodNameToCodeBlock Map of method names to their implementation code blocks
     * @param usedMethods Set of method names that have been used in replacements
     * @return The content with extra methods appended
     */
    private String appendExtraMethods(String content, Map<String, String> methodNameToCodeBlock, Set<String> usedMethods) {
        StringBuilder processedContent = new StringBuilder(content);
        StringBuilder extraMethods = new StringBuilder();

        // Collect extra methods that weren't used
        for (Map.Entry<String, String> entry : methodNameToCodeBlock.entrySet()) {
            if (!usedMethods.contains(entry.getKey())) {
                extraMethods.append("\n\n    // Additional helper method generated by LLM\n    ")
                        .append(entry.getValue());
            }
        }

        // insert extra methods (before the last closing brace)
        if (!extraMethods.isEmpty()) {
            int lastBracePos = processedContent.lastIndexOf("}");
            if (lastBracePos > 0) {
                processedContent.insert(lastBracePos, extraMethods);
                LoggerUtil.info("Added " + (methodNameToCodeBlock.size() - usedMethods.size()) +
                        " extra helper methods from LLM response");
            }
        }

        return processedContent.toString();
    }


    /**
     * Parses methods from an LLM response and returns them as a map of method name to code block.
     *
     * @param llmResponse The LLM response containing method implementations
     * @return A map of method names to their implementation code blocks
     */
    public Map<String, String> parseMethodsFromLLMResponse(String llmResponse) {
        if (llmResponse == null || llmResponse.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> methodMap = new HashMap<>();
        Map<Integer, String> methodStartPositions = detectMethodStartPositions(llmResponse);

        Integer[] positions = methodStartPositions.keySet().toArray(new Integer[0]);
        java.util.Arrays.sort(positions);

        for (int startPos : positions) {
            String methodName = methodStartPositions.get(startPos);
            int endPos = findMethodEnd(llmResponse, startPos);

            if (endPos > startPos) {
                String methodBlock = llmResponse.substring(startPos, endPos).trim();
                methodMap.put(methodName, methodBlock);
            }
        }

        return methodMap;
    }

    /**
     * Detects method declarations and their start positions in the LLM response.
     *
     * @param llmResponse The LLM response containing method implementations
     * @return A map of start positions to method names
     */
    private Map<Integer, String> detectMethodStartPositions(String llmResponse) {
        Map<Integer, String> methodStartPositions = new HashMap<>();

        Pattern methodStartPattern = Pattern.compile(
                "(?m)^\\s*(public|private|protected)\\s+\\w+\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");

        Matcher methodStartMatcher = methodStartPattern.matcher(llmResponse);

        while (methodStartMatcher.find()) {
            methodStartPositions.put(methodStartMatcher.start(), methodStartMatcher.group(2));
        }

        return methodStartPositions;
    }


    /**
     * Extracts a method name from a code block by parsing the method signature.
     *
     * @param codeBlock The code block containing a method
     * @return The extracted method name or null if not found
     */
    private String extractMethodName(String codeBlock) {
        if (codeBlock == null || codeBlock.isEmpty()) {
            return null;
        }

        Pattern methodPattern = Pattern.compile(
                "(?m)^\\s*(public|private|protected|)\\s+(?:\\w+\\s+)?(\\w+)\\s*\\([^)]*\\)",
                Pattern.MULTILINE
        );

        Matcher matcher = methodPattern.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(2);
        }

        return null;
    }

    /**
     * Helper method to find the end of a method by counting opening and closing braces.
     *
     * @param text The text to search in
     * @param startPos The position where the method declaration starts
     * @return The position of the closing brace that ends the method
     */
    private int findMethodEnd(String text, int startPos) {
        int braceCount = 0;
        boolean inMethod = false;

        for (int i = startPos; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '{') {
                braceCount++;
                inMethod = true;
            } else if (c == '}') {
                braceCount--;

                if (inMethod && braceCount == 0) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }
}