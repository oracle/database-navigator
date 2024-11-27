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

package com.dbn.editor.data;

import com.dbn.common.dispose.Disposer;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.data.state.DatasetEditorState;
import com.dbn.object.DBDataset;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBDatasetVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.nn;

public class DatasetEditorProvider implements FileEditorProvider, NamedComponent, DumbAware {
    /*********************************************************
     *                  FileEditorProvider                   *
     *********************************************************/

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            DBContentType contentType = databaseFile.getContentType();
            return contentType == DBContentType.DATA || contentType == DBContentType.CODE_AND_DATA;

        }
        return false;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
        DBDatasetVirtualFile datasetFile = nn(databaseFile.getContentFile(DBContentType.DATA));
        DBDataset dataset = datasetFile.getObject();
        return new DatasetEditor(databaseFile, dataset);
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        // expensive task. start in background
        Disposer.dispose(editor);
    }

    @Override
    @NotNull
    public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile editableObjectFile = (DBEditableObjectVirtualFile) virtualFile;
            DBObjectType objectType = editableObjectFile.getObjectType();
            if (objectType.isInheriting(DBObjectType.DATASET)) {
                DatasetEditorState editorState = new DatasetEditorState();
                editorState.readState(sourceElement);
                return editorState;

            }
        }
        return new DatasetEditorState();
    }

    @Override
    public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
        if (state instanceof DatasetEditorState) {
            DatasetEditorState editorState = (DatasetEditorState) state;
            editorState.writeState(targetElement);
        }
    }

    @Override
    @NotNull
    @NonNls
    public String getEditorTypeId() {
        return EditorProviderId.DATA.getId();
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }


    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/
    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DatasetEditorProvider";
    }
}

