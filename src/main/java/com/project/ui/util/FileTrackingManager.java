package com.project.ui.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages tracking of files in the editor, detects Java files,
 * and notifies subscribers of relevant file changes.
 *
 * @author Sara Moussa
 */
public class FileTrackingManager {


    /**
     * The IntelliJ project instance being tracked by the FileTrackingManager.
     */
    private final Project project;
    /**
     * Manages and tracks file analysis results.
     */
    private final FileAnalysisTracker fileAnalysisTracker;

    /**
     * A list of registered listeners that are notified when a file change occurs.
     */
    private final List<FileChangeListener> fileChangeListeners = new ArrayList<>();

    /**
     * A disposable resource to manage the lifecycle
     * of listeners and subscriptions within the FileTrackingManager.
     */
    private final Disposable disposable;

    /**
     * List of listeners to be notified of file content changes.
     */
    protected final List<FileContentChangeListener> fileContentChangeListeners = new ArrayList<>();

    /**
     * Text area to display the LLM response.
     */
    private final JTextArea llmResponseTextArea;

    /**
     * Constructs a new FileTrackingManager.
     *
     * @param project The current IntelliJ project.
     * @param fileAnalysisTracker The tracker for file analysis results.
     */
    public FileTrackingManager(Project project, FileAnalysisTracker fileAnalysisTracker, JTextArea llmResponseTextArea) {
        this.project = project;
        this.fileAnalysisTracker = fileAnalysisTracker;
        this.llmResponseTextArea = llmResponseTextArea;
        this.disposable = Disposer.newDisposable();
        Disposer.register(project, disposable);

        initializeFileEditorListener();
        initializeDocumentListener();
    }

    /**
     * Initializes the file editor listener to track file selection changes.
     */
    private void initializeFileEditorListener() {
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        notifyListeners();
                    }

                    @Override
                    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        if ("JAVA".equalsIgnoreCase(file.getExtension())) {
                            fileAnalysisTracker.invalidateFileCache(file.getPath());
                            notifyListeners();
                        }
                    }

                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        if (event.getNewFile() != null) {
                            updateLLMResponseForCurrentFile(event.getNewFile().getPath());
                            notifyListeners();
                        }
                    }
                }
        );
    }

    /**
     * Updates the LLM response text area with the cached response for the current file.
     *
     * @param filePath The path of the current file.
     */
    private void updateLLMResponseForCurrentFile(String filePath) {
        if (filePath != null && llmResponseTextArea != null) {
            String cachedResponse = fileAnalysisTracker.getCachedLLMResponse(filePath);
            if (cachedResponse != null) {
                llmResponseTextArea.setText(cachedResponse);
            } else {
                llmResponseTextArea.setText("");
            }
        }
    }

    /**
     * Initializes document listener to track file content changes.
     */
    private void initializeDocumentListener() {
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                Document document = event.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);

                // Only process Java files
                if (file != null && file.getExtension() != null && file.getExtension().equals("java")) {
                    // Clear any cached analysis results for this file
                    fileAnalysisTracker.invalidateFileCache
                            (file.getPath());

                    // Notify listeners
                    for (FileChangeListener listener : fileChangeListeners) {
                        if (listener != null) {
                            listener.onFileChanged(Optional.of(file));
                            if (listener instanceof FileContentChangeListener) {
                                ((FileContentChangeListener) listener).onFileContentChanged(file);
                            }
                        }
                    }
                }
            }
        }, disposable);
    }


    /**
     * Detects the currently active Java file in the editor.
     *
     * @return An Optional containing the current Java file, or empty if no Java file is active.
     */
    public Optional<VirtualFile> detectCurrentJavaFile() {
        VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();

        if (selectedFiles.length > 0) {
            VirtualFile file = selectedFiles[0];
            if ("JAVA".equalsIgnoreCase(file.getExtension())) {
                return Optional.of(file);
            }
        }

        return Optional.empty();
    }


    /**
     * Adds a listener to be notified of file changes.
     *
     * @param listener The listener to add.
     */
    public void addFileChangeListener(FileChangeListener listener) {
        if (!fileChangeListeners.contains(listener)) {
            fileChangeListeners.add(listener);

            if (listener != null) {
                listener.onFileChanged(detectCurrentJavaFile());
            }
        }
    }

    /**
     * Notifies all listeners that a file change has occurred.
     */
    private void notifyListeners() {
        Optional<VirtualFile> currentFile = detectCurrentJavaFile();
        for (FileChangeListener listener : fileChangeListeners) {
            if (listener != null) {
                listener.onFileChanged(currentFile);
            }
        }
    }

    /**
     * Gets the project being tracked by this manager.
     *
     * @return The IntelliJ project instance.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Adds a listener to be notified of file content changes.
     *
     * @param listener The listener to add.
     */
    public void addFileContentChangeListener(FileContentChangeListener listener) {
        fileContentChangeListeners.add(listener);
    }
}