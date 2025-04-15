package com.project.logic.parsing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.project.model.CodeBlockInfo;
import com.project.model.Violation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CodeParser class, which is responsible for parsing Java files
 * and extracting information about code blocks and violations.
 *
 * @author Sara Moussa.
 */
class CodeParserTest {

    /**
     * Validates that the method extractViolatedBlocksInfo returns an empty list
     * when no violations are provided as input.
     * @throws Exception if an error occurs during execution.
     */
    @Test
    void shouldReturnEmptyListWhenNoViolations() throws Exception {
        JavaParser mockParser = mock(JavaParser.class);
        CodeParser codeParser = new CodeParser(mockParser);

        List<CodeBlockInfo> result = codeParser.extractViolatedBlocksInfo("TestFile.java", List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when no violations are passed.");
    }


    /**
     * Verifies that an empty list is returned when the provided file cannot be parsed.
     *
     * @throws Exception if any unexpected error occurs during test execution.
     */
    @Test
    void shouldReturnEmptyListWhenFileIsInvalid() throws Exception {
        JavaParser mockParser = mock(JavaParser.class);
        when(mockParser.parse(anyString())).thenThrow(new RuntimeException("Parsing error"));

        CodeParser codeParser = new CodeParser(mockParser);

        List<Violation> violations = List.of(new Violation(10, "rule1", "message1"));
        List<CodeBlockInfo> result = codeParser.extractViolatedBlocksInfo("InvalidFile.java", violations);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when the file cannot be parsed.");
    }


    /**
     * Ensures that the method correctly handles violations with line numbers outside the range
     * of the declared classes in the provided file.
     *
     * @throws Exception if an error occurs during file parsing or processing.
     */
    @Test
    void shouldFilterViolationsOutsideClassRange() throws Exception {
        String fileContent = """
                public class TestClass {
                    public void method1() {
                        int x = 0;
                    }
                }
                """;

        CompilationUnit mockUnit = mock(CompilationUnit.class);
        when(mockUnit.findAll(any())).thenReturn(List.of());

        JavaParser mockParser = mock(JavaParser.class);
        when(mockParser.parse(anyString())).thenReturn(Mockito.mock(com.github.javaparser.ParseResult.class));
        when(mockParser.parse(anyString()).getResult()).thenReturn(java.util.Optional.of(mockUnit));

        CodeParser codeParser = new CodeParser(mockParser);

        Files.writeString(Path.of("TestFile.java"), fileContent);

        List<Violation> violations = List.of(new Violation(90, "rule1", "message1"));
        List<CodeBlockInfo> result = codeParser.extractViolatedBlocksInfo("TestFile.java", violations);

        assertNotNull(result, "Result should not be null when violations are outside the class range.");
        assertEquals(0, result.size(), "Result should not include violations outside class range.");
    }



    /**
     * Verifies that the parser correctly processes valid input, identifies code blocks,
     * and returns information about blocks that correspond to provided violations.
     *
     * @throws Exception if file operations or parsing fail
     */
    @Test
    void shouldHandleValidInputAndReturnBlocks() throws Exception {

        String fileContent = """
            public class TestClass {
                public void method1() {
                    int x = 0;
                }
            }
            """;
        Path testFile = Files.createTempFile("TestFile", ".java");
        Files.writeString(testFile, fileContent);

        JavaParser realParser = new JavaParser();
        CodeParser codeParser = new CodeParser(realParser);

        List<Violation> violations = List.of(new Violation(3, "rule1", "message1"));

        List<CodeBlockInfo> results = codeParser.extractViolatedBlocksInfo(testFile.toString(), violations);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(block ->
                block.blockType().equals("Method") && block.startLine() <= 3 && block.endLine() >= 3));

        Files.deleteIfExists(testFile);
    }


    /**
     * Verifies expected results for various Node subclasses, including method, class, lambda,
     * static initializer, and an unspecified node type.
     *
     * @throws Exception if an error occurs during reflective access or invocation of the method.
     */
    @Test
    void testDetermineBlockType() throws Exception {
        JavaParser parser = new JavaParser();
        CodeParser codeParser = spy(new CodeParser(parser));

        MethodDeclaration methodNode = new MethodDeclaration();
        ClassOrInterfaceDeclaration classNode = new ClassOrInterfaceDeclaration();
        LambdaExpr lambdaNode = new LambdaExpr();
        InitializerDeclaration initNode = new InitializerDeclaration();
        Node unknownNode = mock(Node.class);

        Method determineBlockType = CodeParser.class.getDeclaredMethod("determineBlockType", Node.class);
        determineBlockType.setAccessible(true);

        assertEquals("Method", determineBlockType.invoke(codeParser, methodNode));
        assertEquals("Class", determineBlockType.invoke(codeParser, classNode));
        assertEquals("Lambda", determineBlockType.invoke(codeParser, lambdaNode));
        assertEquals("StaticBlock", determineBlockType.invoke(codeParser, initNode));
        assertEquals("Unknown", determineBlockType.invoke(codeParser, unknownNode));
    }

}