package com.project.logic.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.project.api.RequestStorage;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Extracts the smallest violated code block from a Java file.
 * Summarizes classes by listing their signature, key methods, and fields.
 *
 * @author Sara Moussa
 */
public class CodeParser {

    /** JavaParser instance for parsing Java source files. */
    private final JavaParser javaParser;

    /**
     * Initializes the extractor with a JavaParser instance.
     *
     * @param javaParser JavaParser for AST analysis.
     */
    public CodeParser(JavaParser javaParser) {
        this.javaParser = javaParser;
    }

    /**
     * Extracts code block information for all violations in a file.
     *
     * @param filePath   The path of the file to analyze.
     * @param violations List of violations detected.
     * @return A list of CodeBlockInfo objects.
     */
    public List<CodeBlockInfo> extractViolatedBlocksInfo(String filePath, List<Violation> violations) {
        List<CodeBlockInfo> blocksInfo = new ArrayList<>();
        if (violations.isEmpty()) {
            return blocksInfo;
        }

        try {
            CompilationUnit compilationUnit = parseJavaFile(filePath);

            // Extract all class declarations in the file
            List<ClassOrInterfaceDeclaration> classDeclarations = compilationUnit.findAll(ClassOrInterfaceDeclaration.class);

            // Track which classes already have a violation entry
            List<Integer> classViolationLines = violations.stream()
                    .map(Violation::lineNumber)
                    .toList();

            var blockMap = groupViolationsByBlock(compilationUnit, filePath, violations);
            blocksInfo.addAll(blockMap.values());

            // Add minimal class summary for each class
            for (ClassOrInterfaceDeclaration clazz : classDeclarations) {
                clazz.getRange().ifPresent(range -> {
                    if (!classViolationLines.contains(range.begin.line)) {
                        String classSummary = ClassSummarizer.summarizeClassMinimal(clazz);
                        blocksInfo.add(0, new CodeBlockInfo(
                                filePath,
                                "Class",
                                range.begin.line,
                                range.end.line,
                                classSummary,
                                new ArrayList<>()
                        ));
                    }
                });
            }

            RequestStorage.clearCodeSnippets();
            blocksInfo.stream()
                    .filter(block -> block.violations() != null && !block.violations().isEmpty())
                    .forEach(block -> RequestStorage.addCodeSnippet(block.codeSnippet()));

            LoggerUtil.info("Extracted " + blocksInfo.size() + " code blocks from " + filePath);
        } catch (Exception e) {
            LoggerUtil.error("Error processing JavaParser for file " + filePath, e);
        }

        return blocksInfo;
    }

    /**
     * Parses the Java source file into an Abstract Syntax Tree (AST).
     *
     * @param filePath The path of the Java file.
     * @return The parsed CompilationUnit.
     * @throws Exception if an error occurs while reading the file.
     */
    private CompilationUnit parseJavaFile(String filePath) throws Exception {
        return javaParser.parse(Files.readString(Path.of(filePath))).getResult().orElseThrow();
    }

    /**
     * Groups violations by their enclosing code blocks.
     *
     * @param compilationUnit The parsed Java file.
     * @param filePath        The file being analyzed.
     * @param violations      List of violations to process.
     * @return A map of unique blocks (start-end) to CodeBlockInfo.
     */
    private Map<String, CodeBlockInfo> groupViolationsByBlock(
            CompilationUnit compilationUnit, String filePath, List<Violation> violations) {

        var blockMap = new java.util.HashMap<String, CodeBlockInfo>();

        for (Violation violation : violations) {
            int violationLine = violation.lineNumber();
            Node block = findEnclosingBlock(compilationUnit, violationLine);

            if (block != null && block.getRange().isPresent()) {
                int begin = block.getRange().get().begin.line;
                int end = block.getRange().get().end.line;
                String key = begin + "-" + end;
                String snippet;
                String blockType = determineBlockType(block);

                // summarize instead of returning the full body.
                if (block instanceof ClassOrInterfaceDeclaration) {
                    snippet = ClassSummarizer.summarizeClass((ClassOrInterfaceDeclaration) block);
                } else {
                    snippet = block.toString();
                }

                if (blockMap.containsKey(key)) {
                    blockMap.get(key).violations().add(violation);
                } else {
                    blockMap.put(key, new CodeBlockInfo(
                            filePath,
                            blockType,
                            begin,
                            end,
                            snippet,
                            new ArrayList<>(List.of(violation))
                    ));
                }
            } else {
                LoggerUtil.warn("No enclosing block found for violation at line " + violationLine);
            }
        }

        return blockMap;
    }

    /**
     * Finds the smallest enclosing block that contains the given violation line.
     *
     * @param unit The parsed Java file.
     * @param line The line number of the violation.
     * @return The smallest enclosing block node.
     */
    private Node findEnclosingBlock(CompilationUnit unit, int line) {
        return unit.accept(new GenericVisitorAdapter<>() {
            @Override
            public Node visit(MethodDeclaration n, Integer line) {
                return bestNode(n, line, super.visit(n, line));
            }

            @Override
            public Node visit(ClassOrInterfaceDeclaration n, Integer line) {
                return bestNode(n, line, super.visit(n, line));
            }

            @Override
            public Node visit(LambdaExpr n, Integer line) {
                return bestNode(n, line, super.visit(n, line));
            }

            @Override
            public Node visit(InitializerDeclaration n, Integer line) {
                return bestNode(n, line, super.visit(n, line));
            }

        }, line);
    }

    /**
     * Determines the smallest suitable node that contains the violation line.
     *
     * @param candidate   Current node candidate.
     * @param line        Line number of the violation.
     * @param currentBest Current best node match.
     * @return Best matching node that minimally encloses the violation line.
     */
    private Node bestNode(Node candidate, int line, Node currentBest) {
        if (candidate.getRange().isEmpty()) return currentBest;
        int begin = candidate.getRange().get().begin.line;
        int end = candidate.getRange().get().end.line;
        if (line < begin || line > end) return currentBest;
        if (currentBest == null || currentBest.getRange().isEmpty()) return candidate;

        int candidateSize = end - begin;
        int bestSize = currentBest.getRange().get().end.line - currentBest.getRange().get().begin.line;
        return candidateSize < bestSize ? candidate : currentBest;
    }

    /**
     * Determines the block type of the given node.
     * @param node The node to analyze.
     * @return The type of the block.
     */
    private String determineBlockType(Node node) {
        if (node instanceof MethodDeclaration) return "Method";
        if (node instanceof ClassOrInterfaceDeclaration) return "Class";
        if (node instanceof LambdaExpr) return "Lambda";
        if (node instanceof InitializerDeclaration) return "StaticBlock";
        return "Unknown";
    }

    /**
     * Inserts marker comments around code blocks containing violations.
     *
     * @param fileContent The original file content as a string
     * @param blocks List of CodeBlockInfo objects containing violations
     * @return A new string with marker comments inserted around flagged blocks
     */
    public String insertMarkers(String fileContent, List<CodeBlockInfo> blocks) {
        if (fileContent == null || blocks == null || blocks.isEmpty()) {
            return fileContent;
        }

        // Split the file into lines
        List<String> lines = new ArrayList<>(List.of(fileContent.split("\n", -1)));

        // Filter blocks that contain violations and sort by start line in descending order
        List<CodeBlockInfo> blocksWithViolations = blocks.stream()
                .filter(block -> block.violations() != null && !block.violations().isEmpty())
                .sorted((b1, b2) -> Integer.compare(b2.startLine(), b1.startLine()))
                .toList();

        // Insert markers for each block with violations
        int uniqueId = 1;
        for (CodeBlockInfo block : blocksWithViolations) {
            int startLine = block.startLine() - 1;
            int endLine = block.endLine();

            // Extract actual method or class name from the code snippet
            String name = extractEntityName(block);
            String markerId = name + "-" + uniqueId;

            // Insert end marker after the block
            if (endLine <= lines.size()) {
                lines.add(endLine, "// <end-flagged:" + markerId + ">");
            }

            // Insert start marker before the block
            if (startLine >= 0) {
                lines.add(startLine, "// <start-flagged:" + markerId + ">");
            }

            uniqueId++;
        }

        return String.join("\n", lines);
    }

    /**
     * Extracts the actual name of the method, class, or other entity from a code block.
     *
     * @param block The CodeBlockInfo containing the code snippet
     * @return The extracted name, or the block type if name cannot be determined
     */
    private String extractEntityName(CodeBlockInfo block) {
        String snippet = block.codeSnippet();
        String blockType = block.blockType();

        if (snippet == null || snippet.isEmpty()) {
            return blockType;
        }

        // Extract method name for Method blocks
        if ("Method".equals(blockType)) {
            java.util.regex.Pattern methodPattern = java.util.regex.Pattern.compile(
                    "\\s+(\\w+)\\s*\\(");
            java.util.regex.Matcher matcher = methodPattern.matcher(snippet);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // Extract class name for Class blocks
        else if ("Class".equals(blockType)) {
            java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile(
                    "class\\s+(\\w+)");
            java.util.regex.Matcher matcher = classPattern.matcher(snippet);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return blockType;
    }
}
