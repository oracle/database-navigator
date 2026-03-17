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

package com.dbn.vector.search;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.DisposableUserDataHolderBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.model.sortable.SortableDataModelState;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.dbn.vector.search.ui.VectorSearchForm;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.Strings.isEmpty;

public class VectorSearchConsole extends DisposableUserDataHolderBase implements FileEditor, DatabaseContextBase, DataProvider {
    private final WeakRef<DBConsoleVirtualFile> databaseFile;

    private VectorSearchForm searchForm;
    private VectorSearchConsoleState state = new VectorSearchConsoleState();

    public VectorSearchConsole(DBConsoleVirtualFile databaseFile) {
        this.databaseFile = WeakRef.of(databaseFile);
        this.searchForm = new VectorSearchForm(this);

        Disposer.register(this, searchForm);
    }

    @NotNull
    public ResultSetTable getSearchResultTable() {
        return getSearchForm().getSearchResultTable();
    }

    @NotNull
    public VectorSearchForm getSearchForm() {
        return Failsafe.nn(searchForm);
    }

    @Nullable
    public ResultSetDataModel getTableModel() {
        return getSearchResultTable().getModel();
    }

    public void refreshTable() {
        ResultSetTable editorTable = getSearchResultTable();
        UserInterface.repaint(editorTable);
        editorTable.adjustColumnWidths();
        //editorTable.restoreSelection();
    }

    public void setSelectedSchema(@Nullable DBSchema schema) {
        if (schema == null) {
            state.setSchemaName(null);
            state.setTableName(null);
        } else if (!Objects.equals(schema.getName(), state.getSchemaName())) {
            state.setSchemaName(schema.getName());
            state.setTableName(null);
        }
    }
    @Nullable
    public DBSchema getSelectedSchema() {
        String schemaName = state.getSchemaName();
        if (isEmpty(schemaName)) return null;

        return getConnection().getObjectBundle().getSchema(schemaName);
    }

    public void setSelectedTable(@Nullable DBTable table) {
        if (table == null) {
            state.setSchemaName(null);
            state.setTableName(null);
        } else {
            state.setSchemaName(table.getSchemaName());
            state.setTableName(table.getName());
        }
    }

    @Nullable
    public DBTable getSelectedTable() {
        String tableName = state.getTableName();
        if (isEmpty(tableName)) return null;

        DBSchema schema = getSelectedSchema();
        if (schema == null) return null;

        return schema.getTable(tableName);
    }

    public void setSelectedMetric(@Nullable DBVectorDistanceMetric distanceMetric) {
        state.setDistanceMetric(distanceMetric);
    }

    public DBVectorDistanceMetric getSelectedMetric() {
        return state.getDistanceMetric();
    }

    @NotNull
    public DBConsoleVirtualFile getDatabaseFile() {
        return databaseFile.ensure();
    }

    @NotNull
    public Project getProject() {
        return getDatabaseFile().getProject();
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return guarded(DISPOSED_COMPONENT, this, e -> e.getSearchForm().getComponent());
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return guarded(null, this, b -> b.getSearchResultTable());
    }

    @Override
    @NonNls
    @NotNull
    public String getName() {
        return "Search";
    }

    @Override
    @NotNull
    public VectorSearchConsoleState getState(@NotNull FileEditorStateLevel level) {
        if (isDisposed() || level != FileEditorStateLevel.FULL) return state;

        SortableDataModelState modelState = readModelState();
        state.setModelState(modelState);
        searchForm.updateState(state);
        return state;
    }

    private @NotNull SortableDataModelState readModelState() {
        ResultSetTable editorTable = getSearchResultTable();
        ResultSetDataModel model = editorTable.getModel();
        return model.getState();
    }

    @Override
    public void setState(@NotNull FileEditorState fileEditorState) {
        if (fileEditorState instanceof VectorSearchConsoleState state) {
            this.state = state;

            ResultSetTable editorTable = getSearchResultTable();
            ResultSetDataModel model = editorTable.getModel();
            model.setState(state.getModelState());
            refreshTable();

            searchForm.applyState(state);
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public boolean isSearching() {
        return getSearchForm().isSearching();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    public int getRowCount() {
        return getSearchResultTable().getRowCount();
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return getDatabaseFile();
    }


    @NotNull
    public ConnectionId getConnectionId() {
        return getDatabaseFile().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getDatabaseFile().getConnection();
    }

    /*******************************************************
     *                   Data Provider                     *
     *******************************************************/

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.VECTOR_SEARCH_CONSOLE.is(dataId)) return this;
        return null;
    }


    @Override
    public void disposeInner() {
        searchForm = null;
        super.disposeInner();
    }
}

