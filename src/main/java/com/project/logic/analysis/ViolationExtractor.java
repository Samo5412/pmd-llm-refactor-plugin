package com.project.logic.analysis;

import com.project.model.Violation;
import com.project.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts violations from PMD's raw output.
 *
 * @author Sara Moussa.
 */
public class ViolationExtractor {

    /**
     * Extracts PMD violations from the provided analysis output.
     *
     * @param pmdOutput The raw output from PMD.
     * @return A list of extracted violations.
     */
    public List<Violation> extractViolations(String pmdOutput) {
        List<Violation> violations = new ArrayList<>();
        Pattern pattern = Pattern.compile("(.+):(\\d+):\\s*([\\w-]+):\\s*(.+)");
        Matcher matcher = pattern.matcher(pmdOutput);

        while (matcher.find()) {
            try {
                int lineNumber = Integer.parseInt(matcher.group(2));
                String ruleName = matcher.group(3);
                String message = matcher.group(4);

                violations.add(new Violation(lineNumber, ruleName, message));
            } catch (NumberFormatException e) {
                LoggerUtil.warn("Skipping invalid violation entry: " + matcher.group());
            }
        }

        LoggerUtil.info("Extracted " + violations.size() + " violations from PMD output.");
        return violations;
    }
}
