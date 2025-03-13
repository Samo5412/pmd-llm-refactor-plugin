package com.project.model;

/**
 * Represents a code violation detected by PMD.
 *
 * @param lineNumber The line number where the violation was detected.
 * @param ruleName   The name of the violated rule.
 * @param message    A description of the violation.
 *
 * @author Sara Moussa.
 */
public record Violation(int lineNumber, String ruleName, String message) {
}
