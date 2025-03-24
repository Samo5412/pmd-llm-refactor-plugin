package com.project.model;

import java.util.List;

/**
 * Represents a batch of code blocks and their result.
 * @param batches List of code block batches.
 * @param userMessage Message to display to the user.
 * @param skippedBlocks List of code blocks that were skipped.
 *
 * @author Sara Moussa.
 */
public record BatchPreparationResult(
        List<List<CodeBlockInfo>> batches,
        String userMessage,
        List<CodeBlockInfo> skippedBlocks
) {}
