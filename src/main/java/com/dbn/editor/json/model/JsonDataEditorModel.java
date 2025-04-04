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

package com.dbn.editor.json.model;

import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.CancellableDatabaseCall;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.DBContentType;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.model.ResultSetAdapter;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.JsonDataEditorState;
import com.dbn.editor.json.ui.JsonDataEditorError;
import com.dbn.editor.json.ui.table.JsonDataEditorTable;
import com.dbn.object.DBDataset;
import com.dbn.object.DBJsonView;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.DELETED;
import static com.dbn.editor.data.model.RecordStatus.DIRTY;
import static com.dbn.editor.data.model.RecordStatus.INSERTED;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;

@Slf4j
public class JsonDataEditorModel
        extends ResultSetDataModel<JsonDataEditorModelRow, JsonDataEditorModelCell>
        implements ListSelectionListener {

    private final boolean resultSetUpdatable;
    private final WeakRef<JsonDataEditor> jsonDataEditor;
    private final DBObjectRef<DBJsonView> jsonView;
    private final DataEditorSettings settings;

    private CancellableDatabaseCall<Object> loaderCall;
    private ResultSetAdapter resultSetAdapter;

    private final List<JsonDataEditorModelRow> changedRows = new ArrayList<>();

    public JsonDataEditorModel(JsonDataEditor jsonDataEditor) throws SQLException {
        super(jsonDataEditor.getConnection());
        Project project = getProject();
        this.jsonDataEditor = WeakRef.of(jsonDataEditor);

        DBJsonView jsonView = jsonDataEditor.getJsonView();
        this.jsonView = DBObjectRef.of(jsonView);
        this.settings =  DataEditorSettings.getInstance(project);
        this.resultSetUpdatable = DatabaseFeature.UPDATABLE_RESULT_SETS.isSupported(getConnection());

        setHeader(new JsonDataEditorModelHeader(jsonDataEditor, null));

        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        boolean readonly = environmentManager.isReadonly(jsonView, DBContentType.DATA);
        setEnvironmentReadonly(readonly);
    }

    public void load(boolean useCurrentFilter, boolean keepChanges) throws SQLException {
        set(DIRTY, false);
        checkDisposed();
        closeResultSet();
        int timeout = getSettings().getGeneralSettings().getFetchTimeout().value();
        AtomicReference<DBNStatement> statementRef = new AtomicReference<>();
        ConnectionHandler connection = getConnection();
        DBNConnection conn = connection.getMainConnection();

        loaderCall = new CancellableDatabaseCall<>(connection, conn, timeout, TimeUnit.SECONDS) {
            @Override
            public Object execute() throws Exception {
                DBNResultSet newResultSet = loadResultSet(useCurrentFilter, statementRef);

                if (newResultSet != null) {
                    checkDisposed();

                    setResultSet(newResultSet);
                    setResultSetExhausted(false);
                    if (keepChanges) snapshotChanges();
                    else clearChanges();

                    int rowCount = computeRowCount();

                    fetchNextRecords(rowCount, true);
                    restoreChanges();
                }
                loaderCall = null;
                return null;
            }

            @Override
            public void cancel() {
                DBNStatement statement = statementRef.get();
                Resources.cancel(statement);
                loaderCall = null;
                set(DIRTY, true);
            }
        };
        loaderCall.start();
    }

    @Override
    protected List<JsonDataEditorModelRow> getChangedRows() {
        return changedRows;
    }

    @Override
    public void setResultSet(DBNResultSet resultSet) {
        super.setResultSet(resultSet);
        resultSetAdapter = Disposer.replace(resultSetAdapter,
                new JsonDataResultSetAdapter(this, resultSet));
        Disposer.register(this, resultSetAdapter);
    }

    @NotNull
    ResultSetAdapter getResultSetAdapter() {
        return Failsafe.nn(resultSetAdapter);
    }

    private int computeRowCount() {
        int originalRowCount = getRowCount();
        int stateRowCount = getState().getRowCount();
        int fetchRowCount = Math.max(stateRowCount, originalRowCount);

        int fetchBlockSize = getSettings().getGeneralSettings().getFetchBlockSize().value();
        fetchRowCount = (fetchRowCount/fetchBlockSize + 1) * fetchBlockSize;

        return Math.max(fetchRowCount, fetchBlockSize);
    }

    public DataEditorSettings getSettings() {
        return Failsafe.nn(settings);
    }

    private DBNResultSet loadResultSet(boolean useCurrentFilter, AtomicReference<DBNStatement> statementRef) throws SQLException {
        int timeout = getSettings().getGeneralSettings().getFetchTimeout().value();
        ConnectionHandler connection = getConnection();
        DBNConnection conn = connection.getMainConnection();
        DBDataset dataset = getJsonView();
        Project project = dataset.getProject();
        DatasetFilter filter = DatasetFilterManager.EMPTY_FILTER;
        if (useCurrentFilter) {
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
            filter = filterManager.getActiveFilter(dataset);
            if (filter == null) filter = DatasetFilterManager.EMPTY_FILTER;
        }

        String selectStatement = filter.createSelectStatement(dataset, getState().getSortingState());
        DBNStatement statement = conn.createStatement();
        statement.setQueryTimeout(timeout);
        statementRef.set(statement);

        checkDisposed();

        statement.setFetchSize(getSettings().getGeneralSettings().getFetchBlockSize().value());
        return statement.executeQuery(selectStatement);
    }

    public boolean isDirty() {
        return is(DIRTY);
    }

    public void cancelDataLoad() {
        if (loaderCall != null) {
            loaderCall.requestCancellation();
        }
    }

    public boolean isLoadCancelled() {
        return loaderCall != null && loaderCall.isCancelRequested();
    }

    private void snapshotChanges() {
        for (JsonDataEditorModelRow row : getRows()) {
            if (row.is(DELETED) || row.isModified() || row.is(INSERTED)) {
                changedRows.add(row);
            }
        }
    }

    private void restoreChanges() {
        if (!hasChanges()) return;

        for (JsonDataEditorModelRow row : getRows()) {
            checkDisposed();

            JsonDataEditorModelRow changedRow = lookupChangedRow(row);
            if (changedRow != null) {
                row.updateStatusFromRow(changedRow);
            }
        }
        setModified(true);
    }

    private JsonDataEditorModelRow lookupChangedRow(JsonDataEditorModelRow row) {
        for (JsonDataEditorModelRow changedRow : changedRows) {
            if (changedRow.isNot(DELETED) && changedRow.matches(row, false)) {
                changedRows.remove(changedRow);
                return changedRow;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public JsonDataEditorState getState() {
        return guarded(JsonDataEditorState.VOID, this, m -> m.getJsonDataEditor().getEditorState());
    }

    private boolean hasChanges() {
        return !changedRows.isEmpty();
    }

    private void clearChanges() {
        changedRows.clear();
        setModified(false);
    }

    @Override
    public boolean isReadonly() {
        return !isEditable();
    }

    public boolean isEditable() {
        return getJsonView().isEditable(DBContentType.JSON_DATA);
    }

    @NotNull
    @Override
    public JsonDataEditorModelHeader getHeader() {
        return (JsonDataEditorModelHeader) super.getHeader();
    }

    @Override
    protected JsonDataEditorModelRow createRow(int resultSetRowIndex) throws SQLException {
        return new JsonDataEditorModelRow(this, getResultSet(), resultSetRowIndex);
    }

    @NotNull
    public DBJsonView getJsonView() {
        return DBObjectRef.ensure(jsonView);
    }

    @NotNull
    public JsonDataEditor getJsonDataEditor() {
        return jsonDataEditor.ensure();
    }

    @NotNull
    public JsonDataEditorTable getEditorTable() {
        return getJsonDataEditor().getEditorTable();
    }

    /****************************************************************
     *                        Editor actions                        *
     ****************************************************************/
    public void deleteRecords(int[] rowIndexes) {
        JsonDataEditorTable editorTable = getEditorTable();
        editorTable.fireEditingCancel();
        DBDataset dataset = getJsonView();
        Progress.prompt(getProject(), dataset, true,
                txt("prc.dataEditor.title.DeletingRecords"),
                txt("prc.dataEditor.text.DeletingRecordsFrom", dataset.getQualifiedNameWithType()),
                progress -> {
            progress.setIndeterminate(false);
            for (int index : rowIndexes) {
                progress.setFraction(Progress.progressOf(index, rowIndexes.length));
                JsonDataEditorModelRow row = getRowAtIndex(index);
                if (progress.isCanceled()) break;

                if (row != null && row.isNot(DELETED)) {
                    int rsRowIndex = row.getResultSetRowIndex();
                    row.delete();
                    if (row.is(DELETED)) {
                        shiftResultSetRowIndex(rsRowIndex, -1);
                        notifyRowUpdated(index);
                    }
                }
                setModified(true);
            }
            DBNConnection conn = getResultConnection();
            conn.notifyDataChanges(dataset.getVirtualFile());
        });
    }

    public void insertRecord(int rowIndex) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        JsonDataEditorTable editorTable = getEditorTable();
        DBDataset dataset = getJsonView();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            resultSetAdapter.startInsertRow();
            JsonDataEditorModelRow newRow = createRow(getRowCount()+1);

            newRow.reset();
            newRow.set(INSERTING, true);
            addRowAtIndex(rowIndex, newRow);
            notifyRowsInserted(rowIndex, rowIndex);

            editorTable.selectCell(rowIndex, editorTable.getSelectedColumn() == -1 ? 0 : editorTable.getSelectedColumn());

            DBNConnection conn = getResultConnection();
            conn.notifyDataChanges(dataset.getVirtualFile());
        } catch (SQLException e) {
            conditionallyLog(e);
            set(INSERTING, false);
            Messages.showErrorDialog(getProject(), "Could not insert record for " + dataset.getQualifiedNameWithType() + ".", e);
        }
    }

    public void duplicateRecord(int rowIndex) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        JsonDataEditorTable editorTable = getEditorTable();
        DBDataset dataset = getJsonView();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            int insertIndex = rowIndex + 1;
            resultSetAdapter.startInsertRow();
            JsonDataEditorModelRow oldRow = getRowAtIndex(rowIndex);
            JsonDataEditorModelRow newRow = createRow(getRowCount() + 1);

            newRow.reset();
            newRow.set(INSERTING, true);
            newRow.updateDataFromRow(oldRow);
            addRowAtIndex(insertIndex, newRow);
            notifyRowsInserted(insertIndex, insertIndex);

            editorTable.selectCell(insertIndex, editorTable.getSelectedColumn());
            DBNConnection conn = getResultConnection();
            conn.notifyDataChanges(dataset.getVirtualFile());
        } catch (SQLException e) {
            conditionallyLog(e);
            set(INSERTING, false);
            Messages.showErrorDialog(getProject(), "Could not duplicate record in " + dataset.getQualifiedNameWithType() + ".", e);
        }
    }

    public void postInsertRecord(boolean propagateError, boolean rebuild, boolean reset) throws SQLException {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        JsonDataEditorTable editorTable = getEditorTable();
        JsonDataEditorModelRow row = getInsertRow();
        if (row == null) return;

        // do not cancel insert-row when focusing away e.g. for copying data from another cell
        if (row.isEmptyData() && !reset) throw AlreadyDisposedException.INSTANCE;

        try {
            editorTable.stopCellEditing();
            resultSetAdapter.insertRow();

            row.reset();
            row.set(INSERTED, true);
            setModified(true);
            set(INSERTING, false);
            if (rebuild) load(true, true);
        } catch (SQLException e) {
            conditionallyLog(e);
            JsonDataEditorError error = new JsonDataEditorError(getConnection(), e);
            if (reset) {
                set(INSERTING, false);
            } else {
                row.notifyError(error, true, true);
            }
            if (!error.isNotified() || propagateError) throw e;
        }
    }

    public void cancelInsert(boolean notifyListeners) {
        ResultSetAdapter resultSetAdapter = getResultSetAdapter();
        JsonDataEditorTable editorTable = getEditorTable();
        try {
            editorTable.fireEditingCancel();
            JsonDataEditorModelRow insertRow = getInsertRow();
            if (insertRow != null) {
                int rowIndex = insertRow.getIndex();
                removeRowAtIndex(rowIndex);
                if (notifyListeners) notifyRowsDeleted(rowIndex, rowIndex);
            }
            resultSetAdapter.cancelInsertRow();
            set(INSERTING, false);
        } catch (SQLException e) {
            conditionallyLog(e);
            log.warn("Failed to cancel insert operation", e);
        }
    }

    /**
     * after delete or insert performed on a result set, the row indexes have to be shifted accordingly
     */
    private void shiftResultSetRowIndex(int fromIndex, int shifting) {
        for (JsonDataEditorModelRow row : getRows()) {
            if (row.getResultSetRowIndex() > fromIndex) {
                row.shiftResultSetRowIndex(shifting);
            }
        }
    }

    @Nullable
    public JsonDataEditorModelRow getInsertRow() {
        for (JsonDataEditorModelRow row : getRows()) {
            if (row.is(INSERTING)) {
                return row;
            }
        }
        return null;
    }

    public int getInsertRowIndex() {
        JsonDataEditorModelRow insertRow = getInsertRow();
        return insertRow == null ? -1 : insertRow.getIndex();
    }

    public void revertChanges() {
        for (JsonDataEditorModelRow row : getRows()) {
            row.revertChanges();
        }
        setModified(false);
    }

    public boolean isResultSetUpdatable() {
        return resultSetUpdatable;
    }

    /*********************************************************
     *                      DataModel                       *
     *********************************************************/
    @Override
    public JsonDataEditorModelCell getCellAt(int rowIndex, int columnIndex) {
        return super.getCellAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        // json view model is not directly editable
    }

    /*********************************************************
     *                ListSelectionListener                  *
     *********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (!is(INSERTING)) return;
        if (event.getValueIsAdjusting()) return;

        JsonDataEditorModelRow insertRow = getInsertRow();
        if (insertRow == null) return;

        int index = insertRow.getIndex();

        ListSelectionModel listSelectionModel = (ListSelectionModel) event.getSource();
        int selectionIndex = listSelectionModel.getLeadSelectionIndex();

        if (index != selectionIndex) {
            //postInsertRecord();
        }
    }
}
