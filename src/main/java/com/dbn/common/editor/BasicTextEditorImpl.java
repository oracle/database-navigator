/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.editor;

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.editor.EditorProviderId;
import com.dbn.vfs.DatabaseOpenFileDescriptor;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.guarded;

public abstract class BasicTextEditorImpl<T extends VirtualFile> extends StatefulDisposableBase implements BasicTextEditor<T>, StatefulDisposable {

    protected TextEditor textEditor;
    private final WeakRef<T> virtualFile;
    private final ProjectRef project;
    private final String name;
    private final EditorProviderId editorProviderId;
    private BasicTextEditorState cachedState;

    public BasicTextEditorImpl(Project project, T virtualFile, String name, EditorProviderId editorProviderId) {
        this.project = ProjectRef.of(project);
        this.name = name;
        this.virtualFile = WeakRef.of(virtualFile);
        this.editorProviderId = editorProviderId;

        TextEditorProvider textEditorProvider = TextEditorProvider.getInstance();
        textEditor = (TextEditor) textEditorProvider.createEditor(project, virtualFile);

        Disposer.register(this, textEditor);
    }

    private boolean isWorkspaceRestore() {
        return ThreadInfo.current().is(ThreadProperty.WORKSPACE_RESTORE);
    }

    @Override
    @NotNull
    public T getVirtualFile() {
        return virtualFile.ensure();
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return virtualFile.get();
    }

    @Override
    public <D> D getUserData(@NotNull Key<D> key) {
        return getTextEditor().getUserData(key);
    }

    @Override
    public <D> void putUserData(@NotNull Key<D> key, D value) {
        getTextEditor().putUserData(key, value);
    }

    @Override
    public boolean isModified() {
        return getTextEditor().isModified();
    }

    @Override
    public boolean isValid() {
        return !isDisposed() && getTextEditor().isValid();
    }

    @Override
    public void selectNotify() {
        getTextEditor().selectNotify();
    }

    @Override
    public void deselectNotify() {
        getTextEditor().deselectNotify();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        getTextEditor().addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        getTextEditor().removePropertyChangeListener(listener);
    }

    @Override
    @Nullable
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return getTextEditor().getBackgroundHighlighter();
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
        return getTextEditor().getCurrentLocation();
    }

    @Override
    @NotNull
    public Editor getEditor() {
        return getTextEditor().getEditor();
    }

    @Override
    public boolean canNavigateTo(@NotNull final Navigatable navigatable) {
        return navigatable instanceof DatabaseOpenFileDescriptor && getTextEditor().canNavigateTo(navigatable);
    }

    @Override
    public void navigateTo(@NotNull final Navigatable navigatable) {
        getTextEditor().navigateTo(navigatable);
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return guarded(DISPOSED_COMPONENT, this, e -> getTextEditor().getComponent());
    }

    @Override
    public EditorProviderId getEditorProviderId() {
        return editorProviderId;
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return guarded(null, this, e -> e.getTextEditor().getPreferredFocusedComponent());
    }

    protected BasicTextEditorState createEditorState() {
        return new BasicTextEditorState();
    }

    @Override
    @NotNull
    public FileEditorState getState(@NotNull FileEditorStateLevel level) {
        if (!isDisposed()) {
            cachedState = createEditorState();
            cachedState.loadFromEditor(level, getTextEditor());
        }
        return cachedState;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        guarded(state, s -> {
            if (s instanceof BasicTextEditorState) {
                BasicTextEditorState editorState = (BasicTextEditorState) s;
                editorState.applyToEditor(getTextEditor());
            }
        });
    }

    @Override
    @NonNls
    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public TextEditor getTextEditor() {
        return Failsafe.nn(textEditor);
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder() {
        return getTextEditor().getStructureViewBuilder();
    }

    @Override
    public String toString() {
        T virtualFile = this.virtualFile.get();
        return virtualFile == null ? super.toString() : virtualFile.getPath();
    }

    @Override
    public void disposeInner() {
        // TODO cleanup - happens as part of text editor disposal
        // EditorUtil.releaseEditor(textEditor.getEditor());
    }


    /*******************************************************************
     *  WORKAROUND: double gutter issue (delegated equals and hashcode)
     *******************************************************************/

    @Override
    @Workaround
    public boolean equals(Object o) {
        return o == this || Objects.equals(o, textEditor);
    }

    @Override
    @Workaround
    public int hashCode() {
        return textEditor.hashCode();
    }

}
