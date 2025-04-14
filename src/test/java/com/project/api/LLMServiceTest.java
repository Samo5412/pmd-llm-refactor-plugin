package com.project.api;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for LLMService to verify response handling.
 *
 * @author Sara Moussa.
 */
class LLMServiceTest {

    /**
     * Verifies that a complete and syntactically valid Java class is correctly identified as valid Java code.
     */
    @Test
    void shouldAcceptCompleteJavaClass() {
        String code = "public class Test { public static void main(String[] args) { System.out.println(\"Hello\"); } }";
        assertTrue(LLMService.isValidJavaCode(code));
    }

    /**
     * Verifies that a fragment of Java code is recognized as valid by the LLMService.
     */
    @Test
    void shouldAcceptJavaMethodFragment() {
        String code = "int sum = 0; for (int i = 0; i < 10; i++) { sum += i; } return sum;";
        assertTrue(LLMService.isValidJavaCode(code));
    }

    /**
     * Verifies that the `isValidJavaCode` method correctly identifies empty or null strings
     * and strings containing only whitespace as invalid Java code.
     */
    @Test
    void shouldRejectEmptyString() {
        Assertions.assertFalse(LLMService.isValidJavaCode(""));
        Assertions.assertFalse(LLMService.isValidJavaCode("   "));
        Assertions.assertFalse(LLMService.isValidJavaCode(null));
    }

    /**
     * Verifies that plain English text without code is correctly identified as invalid Java code.
     */
    @Test
    void shouldRejectPlainEnglishText() {
        String text = "This is just English text with no code at all. It talks about programming concepts.";
        Assertions.assertFalse(LLMService.isValidJavaCode(text));
    }

    /**
     * Verifies that Python code is correctly rejected as invalid Java code.
     */
    @Test
    void shouldRejectPythonCode() {
        String pythonCode = "def calculate(x):\n    return x * 2\n\nresult = calculate(5)\nprint(f\"Result: {result}\")";
        Assertions.assertFalse(LLMService.isValidJavaCode(pythonCode));
    }

    /**
     * Verifies that JavaScript code is correctly identified and rejected as invalid Java code.
     */
    @Test
    void shouldRejectJavaScriptCode() {
        String jsCode = "const add = (a, b) => a + b;\nlet result = add(5, 3);\nconsole.log(`The result is ${result}`);";
        Assertions.assertFalse(LLMService.isValidJavaCode(jsCode));
    }

    /**
     * Verifies that invalid Java code with syntax errors is correctly identified as invalid.
     */
    @Test
    void shouldRejectJavaCodeWithSyntaxErrors() {
        String invalidCode = "public class Test { public static void main(String[] args) { System.out.println(\"Missing semicolon\") }";
        Assertions.assertFalse(LLMService.isValidJavaCode(invalidCode));
    }

    /**
     * Verifies that a string containing Java keywords but with an invalid structure
     * is correctly identified as not being valid Java code.
     */
    @Test
    void shouldHandleCodeWithJavaKeywordsButInvalidStructure() {
        String confusingCode = "public class void return if else; // This has keywords but isn't valid Java";
        Assertions.assertFalse(LLMService.isValidJavaCode(confusingCode));
    }

    /**
     * Tests if the method `isValidJavaCode` correctly identifies valid Java method definitions.
     */
    @Test
    void shouldAcceptMethods() {
        String code = "public void firstMethod() { System.out.println(\"First\"); }\n";
        assertTrue(LLMService.isValidJavaCode(code));
    }

    /**
     * Verifies that the service correctly accepts Java code containing multiple method declarations.
     */
    @Test
    void shouldAcceptMultipleMethods() {
        String code = "public void firstMethod() { System.out.println(\"First\"); }\n" +
                "public int secondMethod(int x) { return x * 2; }";
        assertTrue(LLMService.isValidJavaCode(code));
    }

    /**
     * Verifies that the system correctly identifies valid Java method code containing annotations.
     */
    @Test
    void shouldAcceptMethodsWithAnnotations() {
        String code = "@Override\n" +
                "public String toString() { return \"Custom toString\"; }\n\n" +
                "@Deprecated\n" +
                "public void oldMethod() { /* deprecated implementation */ }";
        assertTrue(LLMService.isValidJavaCode(code));
    }
}