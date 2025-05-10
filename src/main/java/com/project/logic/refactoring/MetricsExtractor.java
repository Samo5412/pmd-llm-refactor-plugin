package com.project.logic.refactoring;

import java.util.*;
import java.util.regex.*;

/**
 * Utility class to extract and calculate metrics from PMD's formatted results.
 * @author Micael Olsson
 */
public class MetricsExtractor {

    /**
     * Private constructor to prevent instantiation.
     */
    private MetricsExtractor() {
    }

    /**
     * Extracts metrics from the formatted results of PMD.
     * @param formattedResults The formatted results from PMD.
     * @return A map where the key is the metric name and the value
     * is a list of integers representing the metric values.
     */
    public static Map<String, List<Integer>> extractMetrics(String formattedResults) {
        Map<String, List<Integer>> metrics = new HashMap<>();
        Pattern pattern = Pattern.compile("- (\\w+): .*? has .*? (\\d+)");
        Matcher matcher = pattern.matcher(formattedResults);

        while (matcher.find()) {
            String metricName = matcher.group(1);
            int value = Integer.parseInt(matcher.group(2));
            metrics.computeIfAbsent(metricName, metricKey -> new ArrayList<>()).add(value);
        }

        return metrics;
    }

    /**
     * Calculates the average and maximum values for each metric extracted from the formatted results.
     * @param formattedResults The formatted results from PMD.
     * @return A map where the key is the metric name and the value is a string
     */
    public static Map<String, String> calculateMetrics(String formattedResults) {
        Map<String, List<Integer>> metrics = extractMetrics(formattedResults);
        Map<String, String> metricsSummary = new HashMap<>();

        int totalMethods = metrics.values().stream().mapToInt(List::size).max().orElse(0);

        // Through each metric name and list of values to calculate the average
        // and maximum values for each metric.
        for (Map.Entry<String, List<Integer>> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            List<Integer> values = entry.getValue();

            double average = values.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            int max = values.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            metricsSummary.put(metricName, String.format("Average: %.2f, Max: %d", average, max));
        }
        metricsSummary.put("Total Methods", String.valueOf(totalMethods));
        return metricsSummary;
    }
}