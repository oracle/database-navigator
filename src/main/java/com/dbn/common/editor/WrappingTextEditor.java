/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;

/**
 * Lightweight {@link TextEditor} adapter for an already-created embedded {@link EditorEx}.
 * <p>
 * IntelliJ platform actions such as Undo and Redo resolve their target through the
 * {@code FileEditor} data context, not only through the focused {@code Editor}. Exposing an
 * embedded editor through this wrapper lets those actions operate on the wrapped editor's
 * document without creating a full file-editor provider.
 */
public class WrappingTextEditor extends UserDataHolderBase implements TextEditor {
    private final EditorEx editor;
    private final String name;

    public WrappingTextEditor(EditorEx editor, String name) {
        this.editor = editor;
        this.name = name;
    }

    @Override
    public @NotNull Editor getEditor() {
        return editor;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return editor.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return editor.getContentComponent();
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return !editor.isDisposed();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public @Nullable BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    public @Nullable StructureViewBuilder getStructureViewBuilder() {
        return null;
    }

    @Override
    public VirtualFile getFile() {
        return editor.getVirtualFile();
    }

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public boolean canNavigateTo(@NotNull Navigatable navigatable) {
        return false;
    }

    @Override
    public void navigateTo(@NotNull Navigatable navigatable) {
    }

    @Override
    public void dispose() {
    }
}
