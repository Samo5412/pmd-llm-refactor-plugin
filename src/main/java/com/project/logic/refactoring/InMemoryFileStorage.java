package com.project.logic.refactoring;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.vfs.VirtualFile;
import com.project.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for storing and managing in-memory file contents.
 *
 * @author Sara Moussa
 */
@Service
public final class InMemoryFileStorage {

    /**
     * A map that stores the original (unchanged) versions of files.
     */
    private final Map<String, String> originalFiles = new HashMap<>();

    /**
     * A map that stores processed versions of files.
     */
    private final Map<String, String> processedFiles = new HashMap<>();

    /**
     * Stores the original version of a file (without any modifications).
     *
     * @param filePath The path to the original file
     * @param content The original content
     */
    public void storeOriginalVersion(String filePath, String content) {
        if (filePath == null || filePath.isEmpty()) {
            LoggerUtil.warn("Attempted to store original version with null or empty file path");
            return;
        }

        originalFiles.put(filePath, content);
        LoggerUtil.debug("Stored original version for file: " + filePath);
    }

    /**
     * Overloaded method that accepts a VirtualFile instead of a file path string.
     *
     * @param file The VirtualFile to store
     * @param content The original content
     */
    public void storeOriginalVersion(VirtualFile file, String content) {
        if (file == null) {
            LoggerUtil.warn("Attempted to store original version with null VirtualFile");
            return;
        }
        storeOriginalVersion(file.getPath(), content);
    }

    /**
     * Retrieves the original version of a file if available.
     *
     * @param filePath The path to the file
     * @return The original version of the file content or null if not found
     */
    public String getOriginalVersion(String filePath) {
        return originalFiles.get(filePath);
    }

    /**
     * Retrieves the original version of a file if available.
     *
     * @param file The VirtualFile to retrieve
     * @return The original version of the file content or null if not found
     */
    public String getOriginalVersion(VirtualFile file) {
        if (file == null) {
            LoggerUtil.warn("Attempted to get original version with null VirtualFile");
            return null;
        }
        return getOriginalVersion(file.getPath());
    }

    /**
     * Stores a processed version of a file with marker blocks replaced by LLM-generated code.
     *
     * @param file The VirtualFile to store
     * @param content The processed content with marker blocks replaced
     */
    public void storeProcessedVersion(VirtualFile file, String content) {
        if (file == null) {
            LoggerUtil.warn("Attempted to store processed version with null VirtualFile");
            return;
        }

        String key = file.getPath();
        processedFiles.put(key, content);
        LoggerUtil.info("Stored processed version of file: " + file.getName());
    }
}
