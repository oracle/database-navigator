package com.dbn.common.editor;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Editors;
import com.dbn.editor.EditorProviderId;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class BasicTextEditorProvider implements FileEditorProvider, NamedComponent, DumbAware {

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        BasicTextEditorState editorState = new BasicTextEditorState();
        virtualFile = getContentVirtualFile(virtualFile);
        editorState.readState(sourceElement, project, virtualFile);
        return editorState;
    }

    public VirtualFile getContentVirtualFile(VirtualFile virtualFile) {
        return virtualFile;
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof BasicTextEditorState) {
            BasicTextEditorState editorState = (BasicTextEditorState) state;
            editorState.writeState(targetElement, project);
        }
    }

    protected void updateTabIcon(final DBEditableObjectVirtualFile databaseFile, final BasicTextEditor textEditor, final Icon icon) {
        Project project = databaseFile.getProject();
        Dispatch.run(() -> Editors.setEditorProviderIcon(project, databaseFile, textEditor, icon));
    }

    @NotNull
    @Override
    public final String getEditorTypeId() {
        return getEditorProviderId().getId();
    }

    @NotNull
    public abstract EditorProviderId getEditorProviderId();

}
