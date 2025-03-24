package com.project.logic;

import com.project.model.BatchPreparationResult;
import com.project.model.CodeBlockInfo;
import com.project.util.LoggerUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Trims code blocks into batches that fit within the character limit.
 *
 * @author Sara Moussa.
 */
public class PromptBatchTrimmer {

    /**
     * Maximum character limit for a single batch.
     */
    private static final int MAX_CHAR_LIMIT = 20000;

    /**
     * Splits a list of code blocks into batches that fit within the character limit.
     *
     * @param allBlocks List of code blocks to split.
     * @return A BatchPreparationResult containing the batches and skipped blocks.
     */
    public static BatchPreparationResult splitIntoBatches(List<CodeBlockInfo> allBlocks) {
        List<List<CodeBlockInfo>> batches = new ArrayList<>();
        List<CodeBlockInfo> skippedBlocks = new ArrayList<>();

        Map<CodeBlockInfo, Queue<CodeBlockInfo>> classToViolations = groupViolationsByClass(allBlocks);
        Queue<Map.Entry<CodeBlockInfo, Queue<CodeBlockInfo>>> queue = new LinkedList<>(classToViolations.entrySet());

        while (!queue.isEmpty()) {
            List<CodeBlockInfo> currentBatch = new ArrayList<>();
            int currentSize = 0;

            Iterator<Map.Entry<CodeBlockInfo, Queue<CodeBlockInfo>>> it = queue.iterator();

            while (it.hasNext()) {
                Map.Entry<CodeBlockInfo, Queue<CodeBlockInfo>> entry = it.next();
                CodeBlockInfo classBlock = entry.getKey();
                Queue<CodeBlockInfo> violations = entry.getValue();

                List<CodeBlockInfo> includedViolations = collectFittingViolations(classBlock, violations, currentSize, skippedBlocks);
                int groupSize = includedViolations.stream().mapToInt(PromptBatchTrimmer::length).sum() + length(classBlock);

                if (!includedViolations.isEmpty()) {
                    currentBatch.add(classBlock);
                    currentBatch.addAll(includedViolations);
                    currentSize += groupSize;

                    LoggerUtil.info("Added class " + classBlock.startLine() + " with " + includedViolations.size() + " block(s) to batch");
                }

                if (violations.isEmpty()) {
                    it.remove();
                } else {
                    LoggerUtil.warn("Deferring remaining block(s) for class starting at line " + classBlock.startLine());
                }

                if (currentSize >= MAX_CHAR_LIMIT) break;
            }

            if (!currentBatch.isEmpty()) {
                batches.add(currentBatch);
                LoggerUtil.info("Created batch with approx. " + currentSize + " chars");
            } else {
                LoggerUtil.warn("Unable to add any blocks in this batch due to size. Skipping batch creation.");
                break;
            }
        }

        String userMessage = generateUserMessage(batches.size(), skippedBlocks);
        return new BatchPreparationResult(batches, userMessage, skippedBlocks);
    }

    /**
     * Groups violations by their containing class.
     *
     * @param allBlocks List of all code blocks.
     * @return A map of class blocks to their corresponding violations.
     */
    private static Map<CodeBlockInfo, Queue<CodeBlockInfo>> groupViolationsByClass(List<CodeBlockInfo> allBlocks) {
        List<CodeBlockInfo> classBlocks = allBlocks.stream()
                .filter(b -> "Class".equals(b.blockType()))
                .toList();

        List<CodeBlockInfo> violationBlocks = allBlocks.stream()
                .filter(b -> !"Class".equals(b.blockType()))
                .toList();

        Map<CodeBlockInfo, Queue<CodeBlockInfo>> map = new LinkedHashMap<>();
        for (CodeBlockInfo classBlock : classBlocks) {
            Queue<CodeBlockInfo> violations = violationBlocks.stream()
                    .filter(v -> v.startLine() >= classBlock.startLine() && v.endLine() <= classBlock.endLine())
                    .collect(Collectors.toCollection(LinkedList::new));
            map.put(classBlock, violations);
        }
        return map;
    }

    /**
     * Collects violations that fit within the character limit.
     *
     * @param classBlock The class block to which the violations belong.
     * @param violations The queue of violations to process.
     * @param currentBatchSize The current size of the batch.
     * @param skippedBlocks List of skipped blocks.
     * @return A list of included violations.
     */
    private static List<CodeBlockInfo> collectFittingViolations(CodeBlockInfo classBlock,
                                                                Queue<CodeBlockInfo> violations,
                                                                int currentBatchSize,
                                                                List<CodeBlockInfo> skippedBlocks) {
        List<CodeBlockInfo> included = new ArrayList<>();
        int groupSize = length(classBlock);

        while (!violations.isEmpty()) {
            CodeBlockInfo block = violations.peek();
            int blockSize = length(block);

            if (blockSize + groupSize > MAX_CHAR_LIMIT) {
                LoggerUtil.warn("Block too large to send (startLine: " + block.startLine() +
                        ", type: " + block.blockType() + "). It will be skipped.");
                skippedBlocks.add(violations.poll());
                continue;
            }

            if (groupSize + blockSize + currentBatchSize > MAX_CHAR_LIMIT) {
                break;
            }

            included.add(violations.poll());
            groupSize += blockSize;
        }

        return included;
    }

    /**
     * Generates a user message summarizing the batch preparation.
     *
     * @param batchCount The number of batches created.
     * @param skipped List of skipped blocks.
     * @return A user-friendly message.
     */
    private static String generateUserMessage(int batchCount, List<CodeBlockInfo> skipped) {
        if (skipped == null) {
            throw new IllegalArgumentException("Skipped blocks list cannot be null");
        }

        StringBuilder sb = new StringBuilder();

        if (batchCount == 0) {
            if (!skipped.isEmpty()) {
                // All code blocks were skipped due to size
                sb.append("Unable to process your code: all ")
                        .append(skipped.size())
                        .append(skipped.size() == 1 ? " block exceeds" : " blocks exceed")
                        .append(" the size limit of ")
                        .append(MAX_CHAR_LIMIT)
                        .append(" characters.");
            } else {
                // No code blocks were found at all
                sb.append("No code blocks were found to process.");
            }
        } else {
            // Some batches were successfully created
            sb.append("Due to size limits, your code was split into ")
                    .append(batchCount)
                    .append(batchCount == 1 ? " batch." : " batches.")
                    .append(System.lineSeparator())
                    .append("All detected issues that fit will be sent for refactoring.");

            // Add information about skipped blocks, if any
            if (!skipped.isEmpty()) {
                sb.append(System.lineSeparator())
                        .append(skipped.size())
                        .append(skipped.size() == 1 ? " block was" : " blocks were")
                        .append(" too large to process and were skipped.");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the length of the code snippet in a block.
     *
     * @param block The code block.
     * @return The length of the code snippet.
     */
    private static int length(CodeBlockInfo block) {
        return block.codeSnippet() != null ? block.codeSnippet().length() : 0;
    }
}