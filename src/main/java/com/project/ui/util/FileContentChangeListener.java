package com.project.ui.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for classes that want to be notified of file content changes.
 *
 * @author Sara Moussa
 */
public interface FileContentChangeListener {
    /**
     * Called when the content of a file changes.
     *
     * @param file The file whose content has changed.
     */
    void onFileContentChanged(@NotNull VirtualFile file);
}