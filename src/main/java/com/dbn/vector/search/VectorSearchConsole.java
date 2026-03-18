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
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.type.DBVectorDistanceMetric;
import com.dbn.vector.search.ui.VectorSearchForm;
import com.dbn.vfs.file.DBSearchConsoleVirtualFile;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
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

    private final WeakRef<DBSearchConsoleVirtualFile> consoleFile;

    private VectorSearchForm searchForm;

    public VectorSearchConsole(DBSearchConsoleVirtualFile consoleFile) {
        this.consoleFile = WeakRef.of(consoleFile);
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

    public void refreshTable() {
        ResultSetTable resultTable = getSearchResultTable();
        UserInterface.repaint(resultTable);
        resultTable.adjustColumnWidths();
        //editorTable.restoreSelection();
    }

    public void setSelectedSchema(@Nullable DBSchema schema) {
        var file = getConsoleFile();
        if (schema == null) {
            file.setSearchSchema(null);
            file.setSearchTable(null);
        } else {
            String schemaName = schema.getName();
            String oldSchemaName = file.getSearchSchema();
            if (!Objects.equals(schemaName, oldSchemaName)) {
                file.setSearchSchema(schemaName);
                file.setSearchTable(null);
            }
        }
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        DBSearchConsoleVirtualFile file = getConsoleFile();
        String schemaName = file.getSearchSchema();
        if (isEmpty(schemaName)) return null;

        return getConnection().getObjectBundle().getSchema(schemaName);
    }

    public void setSelectedTable(@Nullable DBTable table) {
        DBSearchConsoleVirtualFile file = getConsoleFile();
        if (table == null) {
            file.setSearchSchema(null);
            file.setSearchTable(null);
        } else {
            file.setSearchSchema(table.getSchemaName());
            file.setSearchTable(table.getName());
        }
    }

    @Nullable
    public DBTable getSelectedTable() {
        DBSearchConsoleVirtualFile file = getConsoleFile();
        String tableName = file.getSearchTable();
        if (isEmpty(tableName)) return null;

        DBSchema schema = getSelectedSchema();
        if (schema == null) return null;

        return schema.getTable(tableName);
    }

    public void setSelectedMetric(@Nullable DBVectorDistanceMetric distanceMetric) {
        DBSearchConsoleVirtualFile file = getConsoleFile();
        file.setDistanceMetric(distanceMetric == null ? null : distanceMetric.id());
    }

    public DBVectorDistanceMetric getSelectedMetric() {
        DBSearchConsoleVirtualFile file = getConsoleFile();
        return DBVectorDistanceMetric.get(file.getDistanceMetric());
    }

    @NotNull
    public DBSearchConsoleVirtualFile getConsoleFile() {
        return consoleFile.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConsoleFile().getProject();
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
    public void setState(@NotNull FileEditorState fileEditorState) {
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
        return getConsoleFile();
    }


    @NotNull
    public ConnectionId getConnectionId() {
        return getConsoleFile().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getConsoleFile().getConnection();
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

