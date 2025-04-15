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

package com.dbn.editor.data;

import com.dbn.common.dispose.DisposableUserDataHolderBase;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.editor.data.DataEditorStatus.CONNECTED;
import static com.dbn.editor.data.DataEditorStatus.LOADED;
import static com.dbn.editor.data.DataEditorStatus.LOADING;


@Getter
@Setter
public abstract class DataEditorBase<T extends DBDataset> extends DisposableUserDataHolderBase implements
        FileEditor,
        DatabaseContextBase,
        DataProvider,
        StatefulDisposable {

    protected static final DataLoadInstructions COL_VISIBILITY_STATUS_CHANGE_LOAD_INSTRUCTIONS = new DataLoadInstructions(DataLoadInstruction.USE_CURRENT_FILTER, DataLoadInstruction.PRESERVE_CHANGES, DataLoadInstruction.DELIBERATE_ACTION, DataLoadInstruction.REBUILD);
    protected static final DataLoadInstructions CON_STATUS_CHANGE_LOAD_INSTRUCTIONS = new DataLoadInstructions(DataLoadInstruction.USE_CURRENT_FILTER);

    private final DBObjectRef<T> dataset;
    private final ProjectRef project;
    private final DBEditableObjectVirtualFile databaseFile;
    private final ConnectionRef connection;

    protected final DataEditorStatusHolder status;
    private String dataLoadError;

    protected DataEditorBase(DBEditableObjectVirtualFile databaseFile, T dataset) {
        this.dataset = DBObjectRef.of(dataset);
        this.databaseFile = databaseFile;
        this.project = ProjectRef.of(dataset.getProject());
        this.connection = ConnectionRef.of(dataset.getConnection());

        this.status = new DataEditorStatusHolder();
        this.status.set(CONNECTED, true);
    }

    @NotNull
    public T getDataset() {
        return DBObjectRef.ensure(dataset);
    }

    @NotNull
    public final DBEditableObjectVirtualFile getDatabaseFile() {
        return nd(databaseFile);
    }

    @NotNull
    public final Project getProject() {
        return project.ensure();
    }

    @Override
    @Nullable
    public final SchemaId getSchemaId() {
        return getDataset().getSchemaId();
    }

    @Override
    @NotNull
    public final ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public final DatabaseSession getSession() {
        return getConnection().getSessionBundle().getMainSession();
    }

    @Nullable
    @Override
    public final VirtualFile getFile() {
        return databaseFile;
    }

    @NotNull
    public abstract ResultSetDataModel getTableModel();

    public final boolean isConnected() {
        return getStatus().is(DataEditorStatus.CONNECTED);
    }

    @Override
    public final boolean isModified() {
        return getTableModel().isModified();
    }

    @Override
    public final boolean isValid() {
        return !isDisposed();
    }

    public final boolean isLoading() {
        return status.is(LOADING);
    }

    public final boolean isLoaded() {
        return status.is(LOADED);
    }

    @Override
    public final void selectNotify() {}

    @Override
    public final void deselectNotify() {}

    @Override
    public final void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    public final void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

    @Override
    @Nullable
    public final BackgroundEditorHighlighter getBackgroundHighlighter() {
        return null;
    }

    @Override
    @Nullable
    public final FileEditorLocation getCurrentLocation() {
        return null;
    }

    protected void focusEditor() {
        Editors.openFileEditor(getProject(), getDatabaseFile(), true);
    }


    @Override
    public final String toString() {
        DBEditableObjectVirtualFile databaseFile = this.databaseFile;
        if (databaseFile == null) return DatabaseFileSystem.createObjectPath(dataset);
        return databaseFile.getPath();
    }
}
