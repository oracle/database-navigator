package com.dbn.common.listener;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.guarded;

public class DBNFileEditorManagerListener implements FileEditorManagerListener {

    @Override
    public final void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        guarded(this, l -> l.whenFileOpened(source, file));
    }

    @Override
    public final void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        guarded(this, l -> l.whenFileClosed(source, file));
    }

    @Override
    public final void selectionChanged(@NotNull FileEditorManagerEvent event) {
        guarded(this, l -> l.whenSelectionChanged(event));
    }

    public void whenFileOpened(FileEditorManager source, VirtualFile file) {}
    public void whenFileClosed(FileEditorManager source, VirtualFile file) {}
    public void whenSelectionChanged(FileEditorManagerEvent event) {}
}
