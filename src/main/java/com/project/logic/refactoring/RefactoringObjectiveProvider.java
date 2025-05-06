package com.project.logic.refactoring;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides refactoring objectives based on detected complexity violations.
 * If multiple violations exist for a code block, it merges them into a single refactoring objective.
 *
 * @author Sara Moussa.
 */
public class RefactoringObjectiveProvider {

    /** Private constructor to prevent instantiation */
    private RefactoringObjectiveProvider() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a combined refactoring objective based on multiple detected violations.
     *
     * @param ruleNames A list of PMD rule violations detected for the code block.
     * @return A single, structured refactoring objective.
     */
    public static String getRefactoringObjective(Set<String> ruleNames) {
        if (ruleNames.isEmpty()) {
            return "No complexity issues detected.";
        }

        // Map individual rules to improvement suggestions
        List<String> objectives = ruleNames.stream()
                .map(RefactoringObjectiveProvider::getObjectiveForRule)
                .distinct()
                .collect(Collectors.toList());

        return "To reduce code complexity while preserving external behavior: " + String.join(" ", objectives);
    }

    /**
     * Returns a refactoring objective based on the detected PMD rule.
     *
     * @param ruleName The name of the PMD rule that was violated.
     * @return A clear objective guiding the LLM on how to refactor the code.
     */
    private static String getObjectiveForRule(String ruleName) {
        return switch (ruleName) {
            case "CyclomaticComplexity" ->
                    "Reduce excessive branching by breaking down complex logic into smaller, reusable methods.";
            case "CognitiveComplexity" ->
                    "Refactor deeply nested structures and simplify conditional logic to enhance readability.";
            case "NPathComplexity" ->
                    "Minimize execution paths by restructuring conditionals and avoiding unnecessary nesting.";
            case "ExcessiveLinesOfCode" ->
                    "Break down large methods into smaller, focused methods that are easier to maintain.";
            default ->
                    "Refactor the code to improve maintainability and reduce complexity while preserving functionality.";
        };
    }

    /**
     * Provides a concise instruction for the LLM to follow when refactoring the code.
     * Emphasizes reducing complexity while preserving functionality.
     *
     * @return the instruction string for the LLM prompt.
     */
    public static String getLLMInstruction() {
        return "Based on the above JSON context, refactor each code unit "
                + "to reduce code complexity while "
                + "preserving external behavior. Return only the refactored code for each unit. "
                + "Do not include any additional explanations, or non-code text.";
    }
}
