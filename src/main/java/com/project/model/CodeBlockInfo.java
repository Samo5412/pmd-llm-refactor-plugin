package com.project.model;

import java.util.List;

/**
 * Represents a code block and its associated violations.
 * @param filePath Path of the file containing the code block.
 * @param blockType Type of code block.
 * @param startLine Start line of the code block.
 * @param endLine End line of the code block.
 * @param codeSnippet The extracted code snippet.
 * @param violations List of violations associated with the code block.
 *
 * @author Sara Moussa.
 */
public record CodeBlockInfo(String filePath, String blockType, int startLine, int endLine, String codeSnippet,
                            List<Violation> violations) {
}
