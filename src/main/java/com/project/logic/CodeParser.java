package com.project.logic;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            var blockMap = groupViolationsByBlock(compilationUnit, filePath, violations);

            blocksInfo.addAll(blockMap.values());
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
                    snippet = summarizeClass((ClassOrInterfaceDeclaration) block);
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

            @Override
            public Node visit(BlockStmt n, Integer line) {
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
        if (node instanceof BlockStmt) return "Block";
        return "Unknown";
    }

    /**
     * Summarizes a class by returning its signature and key method/field declarations.
     *
     * @param clazz The class to summarize.
     * @return A summary string containing the class signature and key method/field declarations.
     */
    private String summarizeClass(ClassOrInterfaceDeclaration clazz) {
        String fields = clazz.getFields().stream()
                .map(f -> f.getElementType().asString() + " " + f.getVariables().stream()
                        .map(NodeWithSimpleName::getNameAsString)
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n    ", "\n  Fields:\n    ", ""));

        String methods = clazz.getMethods().stream()
                .map(this::summarizeMethod)
                .collect(Collectors.joining("\n    ", "\n  Methods:\n    ", ""));

        return String.format("%s class %s {%s%s\n}",
                clazz.getAccessSpecifier().asString().trim(),
                clazz.getNameAsString(),
                fields.isBlank() ? "" : fields,
                methods.isBlank() ? "" : methods
        );
    }

    /**
     * Summarizes a method by returning its signature.
     *
     * @param method The MethodDeclaration object representing the method to summarize.
     * @return The signature of the method as a string.
     */
    private String summarizeMethod(MethodDeclaration method) {
        String modifiers = method.getModifiers().stream().map(Object::toString).collect(Collectors.joining(" ")).trim();
        String params = method.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", "));

        return String.format("%s %s %s(%s)", modifiers, method.getType().asString(), method.getNameAsString(), params).trim();
    }
}
