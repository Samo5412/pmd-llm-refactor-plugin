package com.project.logic;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FileDetector {

    public static Optional<VirtualFile> detectCurrentJavaFile(Project project) {
        VirtualFile[] openFiles = FileEditorManager.getInstance(project).getSelectedFiles();

        if (openFiles.length > 0) {
            VirtualFile currentFile = openFiles[0];
            if ("java".equalsIgnoreCase(currentFile.getExtension())) {
                return Optional.of(currentFile);
            }
        }

        // If no file or if it's not Java, return empty
        return Optional.empty();
    }

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

