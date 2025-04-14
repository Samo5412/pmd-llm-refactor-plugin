package com.project.logic.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code PMDRunner} class to verify its behavior with various inputs.
 *
 * @author Sara Moussa
 */
class PMDRunnerTest {

    /**
     * Manages an instance of {@code PMDRunner} for analyzing Java source code.
     */
    private PMDRunner pmdRunner;

    /**
     * Initializes the {@code PMDRunner} instance before each test.
     */
    @BeforeEach
    void setUp() {
        pmdRunner = new PMDRunner();
    }

    /**
     * Verifies that the PMDRunner correctly handles empty source code input.
     */
    @Test
    void shouldHandleEmptySourceCodeInput() {
        String result = pmdRunner.runPMDOnString("", "Empty.java");

        assertEquals("No source code provided for PMD analysis.", result,
                "Should return appropriate message for empty source code");
    }

    /**
     * Validates that null input for source code is handled gracefully by the PMD runner.
     */
    @Test
    void shouldHandleNullSourceCodeInput() {
        String result = pmdRunner.runPMDOnString(null, "Null.java");

        assertEquals("No source code provided for PMD analysis.", result,
                "Should return appropriate message for null source code");
    }

    /**
     * Verifies that PMD analysis does not report any violations for a Java class
     * containing clean, well-written code.
     *
     * @param tempDir A temporary directory for writing the Java source file.
     * @throws Exception If an error occurs while writing the file or running the analysis.
     */
    @Test
    void shouldAnalyzeCleanCode(@TempDir Path tempDir) throws Exception {
        Path javaFile = tempDir.resolve("CleanClass.java");
        String cleanCode =
                "public class CleanClass {\n" +
                        "    public void method() {\n" +
                        "        int usedVar = 5;\n" +
                        "        System.out.println(usedVar);\n" +
                        "    }\n" +
                        "}";
        Files.writeString(javaFile, cleanCode);

        String result = pmdRunner.runPMD(javaFile.toString());

        assertFalse(result.contains("Violation"),
                "Clean code should not trigger violations");
    }

    /**
     * Verifies that PMD analysis correctly identifies a violation in a Java class
     */
    @Test
    void shouldReportCognitiveComplexityViolation() {
        String complexCode =
                "public class ComplexClass {\n" +
                        "    public void complexMethod(int a, int b, int c, int d, int e) {\n" +
                        "        if (a > 0) {\n" +
                        "            if (b > 0) {\n" +
                        "                if (c > 0) {\n" +
                        "                    if (d > 0) {\n" +
                        "                        if (e > 0) {\n" +
                        "                            System.out.println(\"All positive\");\n" +
                        "                        } else {\n" +
                        "                            System.out.println(\"E not positive\");\n" +
                        "                        }\n" +
                        "                    } else {\n" +
                        "                        System.out.println(\"D not positive\");\n" +
                        "                    }\n" +
                        "                } else {\n" +
                        "                    System.out.println(\"C not positive\");\n" +
                        "                }\n" +
                        "            } else {\n" +
                        "                System.out.println(\"B not positive\");\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            System.out.println(\"A not positive\");\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";

        String result = pmdRunner.runPMDOnString(complexCode, "ComplexClass.java");

        assertTrue(result.contains("CognitiveComplexity"),
                "PMD should detect high cognitive complexity but got: " + result);
    }
}