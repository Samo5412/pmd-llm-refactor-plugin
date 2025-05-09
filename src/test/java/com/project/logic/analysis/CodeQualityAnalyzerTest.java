package com.project.logic.analysis;

import com.project.logic.refactoring.CodeQualityAnalyzer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for the {@code CodeQualityAnalyzer} class.
 */
class CodeQualityAnalyzerTest {

    /**
     * Tests the creation of a temporary ruleset for PMD analysis.
     */
    @Test
    void testCreateTemporaryRuleset() throws IOException {
        CodeQualityAnalyzer analyzer = new CodeQualityAnalyzer();
        Path tempRulesetPath = analyzer.createTemporaryRuleset();
        PMDRunner pmdRunner = new PMDRunner();
        pmdRunner.setTemporaryRuleSet(tempRulesetPath.toString());

        String mockMethod = """
            public class MockClass {
                public void mockMethod() {
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 10; j++) {
                            System.out.println(i * j);
                        }
                    }
                }
            }
        """;
        Path tempDir = Files.createTempDirectory("mock_test_");
        Path mockFilePath = tempDir.resolve("MockClass.java");
        Files.writeString(mockFilePath, mockMethod);

        String pmdOutput = pmdRunner.runPMD(mockFilePath.toString());

        assertNotNull(pmdOutput, "PMD output should not be null.");
        assertTrue(pmdOutput.contains("CyclomaticComplexity"), "PMD output should report CyclomaticComplexity violations.");
        assertTrue(pmdOutput.contains("NPathComplexity"), "PMD output should report NPathComplexity violations.");

        Files.deleteIfExists(mockFilePath);
        Files.deleteIfExists(tempRulesetPath);
        Files.deleteIfExists(tempDir);
    }
}