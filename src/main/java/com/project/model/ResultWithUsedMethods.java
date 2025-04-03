package com.project.model;

import java.util.Set;

/**
 * Helper class to return both modified content and the set of used methods.
 *
 * @author Sara Moussa.
 */
public record ResultWithUsedMethods(String content, Set<String> usedMethods) {
}