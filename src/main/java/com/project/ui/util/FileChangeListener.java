package com.project.ui.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Interface for classes that want to be notified of file changes.
 *
 * @author Sara Moussa
 */
public interface FileChangeListener {
    /**
     * Called when the active Java file changes or is updated.
     *
     * @param file The active Java file, or empty if no Java file is active.
     */
    void onFileChanged(@NotNull Optional<VirtualFile> file);
}
