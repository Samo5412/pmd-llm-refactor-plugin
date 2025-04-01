package com.project.ui.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks and caches file analysis results.
 *
 * @author Sara Moussa
 */
public class FileAnalysisTracker {

    /**
     * Cache for analyzed files and their issue status.
     * Key: File path, Value: Boolean indicating if the file has issues.
     */
    private final Map<String, Boolean> analyzedFilesCache;

    /**
     * Cache for PMD analysis results.
     * Key: File path, Value: PMD result string.
     */
    private final Map<String, String> pmdResultCache;

    /**
     * Cache for LLM responses.
     * Key: File path, Value: LLM response string.
     */
    private final Map<String, String> llmResponseCache;

    /**
     * Constructs a new FileAnalysisTracker with initialized caches.
     */
    public FileAnalysisTracker() {
        analyzedFilesCache = new HashMap<>();
        pmdResultCache = new HashMap<>();
        llmResponseCache = new HashMap<>();
    }

    /**
     * Caches the analysis result for a file.
     *
     * @param filePath the path of the analyzed file
     * @param hasIssues indicates if the file has issues
     * @param pmdResult the PMD analysis result
     */
    public void cacheAnalysisResult(String filePath, boolean hasIssues, String pmdResult) {
        analyzedFilesCache.put(filePath, hasIssues);
        pmdResultCache.put(filePath, pmdResult);
    }

    /**
     * Caches the LLM response for a file.
     *
     * @param filePath the path of the file
     * @param response the LLM response
     */
    public void cacheLLMResponse(String filePath, String response) {
        llmResponseCache.put(filePath, response);
    }

    /**
     * Retrieves the cached analysis result for a file.
     *
     * @param filePath the path of the file
     * @return a Boolean indicating if the file has issues
     */
    public Boolean getCachedAnalysisResult(String filePath) {
        return analyzedFilesCache.get(filePath);
    }

    /**
     * Retrieves the cached LLM response for a file.
     *
     * @param filePath the path of the file
     * @return the cached LLM response
     */
    public String getCachedLLMResponse(String filePath) {
        return llmResponseCache.get(filePath);
    }

    /**
     * Invalidates all cached information for a file.
     *
     * @param filePath the path of the file to invalidate
     */
    public void invalidateFileCache(String filePath) {
        analyzedFilesCache.remove(filePath);
        pmdResultCache.remove(filePath);
        llmResponseCache.remove(filePath);
    }

    /**
     * Retrieves the cached PMD result for a file.
     *
     * @param filePath the path of the file
     * @return the cached PMD result
     */
    public String getCachedPmdResult(String filePath) {
        return pmdResultCache.get(filePath);
    }

    /**
     * Invalidates only the LLM response for a file, preserving other cached data.
     *
     * @param filePath the path of the file to invalidate LLM response for
     */
    public void invalidateLLMResponse(String filePath) {
        if (filePath != null) {
            llmResponseCache.remove(filePath);
        }
    }
}