package com.project.logic.refactoring;

/**
 * Provides refactoring objectives based on detected complexity violations.
 *
 * @author Sara Moussa.
 */
public class RefactoringObjectiveProvider {

    /** Private constructor to prevent instantiation */
    private RefactoringObjectiveProvider() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns a refactoring objective based on the detected PMD rule.
     *
     * @param ruleName The name of the PMD rule that was violated.
     * @return A clear objective guiding the LLM on how to improve the code.
     */
    public static String getRefactoringObjective(String ruleName) {
        return switch (ruleName) {
            case "CyclomaticComplexity" ->
                    "Reduce branching and split complex logic into smaller, reusable methods while preserving functionality.";
            case "CognitiveComplexity" ->
                    "Refactor deeply nested structures, simplify conditions, and improve readability while maintaining correctness.";
            case "NPathComplexity" ->
                    "Reduce execution paths by simplifying conditionals and avoiding redundant logic without altering behavior.";
            case "ExcessivePublicCount" ->
                    "Consider reducing the number of public methods by encapsulating logic within private methods where possible.";
            default ->
                    "Refactor this code to improve maintainability and reduce complexity while ensuring correctness.";
        };
    }
}
