package com.project.logic.util;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Utility class for detecting the currently opened Java file in an IntelliJ project.
 *
 * @author Sara Moussa
 */
public class FileDetector {

    /**
     * Detects the currently selected Java file in the project.
     *
     * @param project The IntelliJ project instance.
     * @return An {@link Optional} containing the detected Java file,
     * or empty if none is found.
     */
    public static Optional<VirtualFile> detectCurrentJavaFile(Project project) {
        VirtualFile[] openFiles = FileEditorManager.getInstance(project).getSelectedFiles();

        if (openFiles.length > 0) {
            VirtualFile currentFile = openFiles[0];

            // Check if the selected file is a Java file
            if ("java".equalsIgnoreCase(currentFile.getExtension())) {
                return Optional.of(currentFile);
            }
        }

        return Optional.empty();
    }

    /**
     * Registers a listener to detect changes in file selection.
     * Triggers the provided callback when the selected file changes.
     *
     * @param project     The IntelliJ project instance.
     * @param onFileChange A callback function to execute when file selection changes.
     */
    public static void registerFileListener(Project project, Runnable onFileChange) {
        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        onFileChange.run();
                    }
                }
        );
    }
}