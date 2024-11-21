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

package com.dbn.editor.data.model;

import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.CancellableDatabaseCall;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionProperties;
import com.dbn.connection.Resources;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.DBContentType;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.DatasetEditorError;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.data.state.DatasetEditorState;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDataset;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.connection.ConnectionProperty.RS_TYPE_FORWARD_ONLY;
import static com.dbn.connection.ConnectionProperty.RS_TYPE_SCROLL_INSENSITIVE;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.DELETED;
import static com.dbn.editor.data.model.RecordStatus.DIRTY;
import static com.dbn.editor.data.model.RecordStatus.INSERTED;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;

@Slf4j
public class DatasetEditorModel
        extends ResultSetDataModel<DatasetEditorModelRow, DatasetEditorModelCell>
        implements ListSelectionListener {

    private final boolean isResultSetUpdatable;
    private final WeakRef<DatasetEditor> datasetEditor;
    private final DBObjectRef<DBDataset> dataset;
    private final DataEditorSettings settings;

    private CancellableDatabaseCall<Object> loaderCall;
    private ResultSetAdapter resultSetAdapter;

    private final List<DatasetEditorModelRow> changedRows = new ArrayList<>();
    private final Latent<List<DBColumn>> uniqueKeyColumns = Latent.basic(() -> loadUniqueKeyColumns());

    public DatasetEditorModel(DatasetEditor datasetEditor) throws SQLException {
        super(datasetEditor.getConnection());
        Project project = getProject();
        this.datasetEditor = WeakRef.of(datasetEditor);
        DBDataset dataset = datasetEditor.getDataset();
        this.dataset = DBObjectRef.of(dataset);
        this.settings =  DataEditorSettings.getInstance(project);
        setHeader(new DatasetEditorModelHeader(datasetEditor, null));
        this.isResultSetUpdatable = DatabaseFeature.UPDATABLE_RESULT_SETS.isSupported(getConnection());

        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        boolean readonly = environmentManager.isReadonly(dataset, DBContentType.DATA);
        setEnvironmentReadonly(readonly);
    }

    public void load(final boolean useCurrentFilter, final boolean keepChanges) throws SQLException {
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
                    setHeader(new DatasetEditorModelHeader(getDatasetEditor(), newResultSet));

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
    protected List<DatasetEditorModelRow> getChangedRows() {
        return changedRows;
    }

    @Override
    public void setResultSet(DBNResultSet resultSet) {
        super.setResultSet(resultSet);

        ConnectionHandler connection = getConnection();
        resultSetAdapter = Disposer.replace(resultSetAdapter,
                DatabaseFeature.UPDATABLE_RESULT_SETS.isSupported(connection) ?
                    new EditableResultSetAdapter(this, resultSet) :
                    new ReadonlyResultSetAdapter(this, resultSet));

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
        DBDataset dataset = getDataset();
        Project project = dataset.getProject();
        DatasetFilter filter = DatasetFilterManager.EMPTY_FILTER;
        if (useCurrentFilter) {
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
            filter = filterManager.getActiveFilter(dataset);
            if (filter == null) filter = DatasetFilterManager.EMPTY_FILTER;
        }

        String selectStatement = filter.createSelectStatement(dataset, getState().getSortingState());
        DBNStatement statement = null;
        if (isReadonly()) {
            statement = conn.createStatement();
        } else {
            // ensure we always get a statement,
            ConnectionProperties properties = conn.getProperties();
            if (properties.is(RS_TYPE_SCROLL_INSENSITIVE)) {
                try {
                    statement = conn.createStatement(
                            ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_UPDATABLE);

                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.warn("Failed to create SCROLL_INSENSITIVE statement: {}", e.getMessage());
                }
            }

            if (statement == null && properties.is(RS_TYPE_FORWARD_ONLY)) {
                try {
                    statement = conn.createStatement(
                            ResultSet.TYPE_FORWARD_ONLY,
                            ResultSet.CONCUR_READ_ONLY);
                } catch (Throwable e) {
                    conditionallyLog(e);
                    log.warn("Failed to create FORWARD_ONLY statement: {}", e.getMessage());
                }
            }

            if (statement == null) {
                // default statement creation
                statement = conn.createStatement();
            }
        }
        statementRef.set(statement);
        checkDisposed();
        if (timeout != -1) {
            statement.setQueryTimeout(timeout);
        }

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
        for (DatasetEditorModelRow row : getRows()) {
            if (row.is(DELETED) || row.isModified() || row.is(INSERTED)) {
                changedRows.add(row);
            }
        }
    }

    private void restoreChanges() {
        if (!hasChanges()) return;

        for (DatasetEditorModelRow row : getRows()) {
            checkDisposed();

            DatasetEditorModelRow changedRow = lookupChangedRow(row);
            if (changedRow != null) {
                row.updateStatusFromRow(changedRow);
            }
        }
        setModified(true);
    }

    private DatasetEditorModelRow lookupChangedRow(DatasetEditorModelRow row) {
        for (DatasetEditorModelRow changedRow : changedRows) {
            if (changedRow.isNot(DELETED) && changedRow.matches(row, false)) {
                changedRows.remove(changedRow);
                return changedRow;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public DatasetEditorState getState() {
        return guarded(DatasetEditorState.VOID, this, m -> m.getDatasetEditor().getEditorState());
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
        return getDataset().isEditable(DBContentType.DATA);
    }

    @NotNull
    @Override
    public DatasetEditorModelHeader getHeader() {
        return (DatasetEditorModelHeader) super.getHeader();
    }

    @Override
    protected DatasetEditorModelRow createRow(int resultSetRowIndex) throws SQLException {
        return new DatasetEditorModelRow(this, getResultSet(), resultSetRowIndex);
    }

    @NotNull
    public DBDataset getDataset() {
        return Failsafe.nn(DBObjectRef.get(dataset));
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    @NotNull
    public DatasetEditorTable getEditorTable() {
        return getDatasetEditor().getEditorTable();
    }

    @Nullable
    public DatasetFilterInput resolveForeignKeyRecord(DatasetEditorModelCell cell) {
        DBColumn column = cell.getColumn();
        if (!column.isForeignKey()) return null;

        for (DBConstraint constraint : column.getConstraints()) {
            constraint = constraint.getUndisposedEntity();
            if (constraint == null || !constraint.isForeignKey()) continue;

            DBConstraint fkConstraint = constraint.getForeignKeyConstraint();
            if (fkConstraint == null) continue;

            DBDataset fkDataset = fkConstraint.getDataset();
            DatasetFilterInput filterInput = new DatasetFilterInput(fkDataset);

            for (DBColumn constraintColumn : constraint.getColumns()) {
                constraintColumn = constraintColumn.getUndisposedEntity();
                if (constraintColumn != null) {
                    DBColumn foreignKeyColumn = constraintColumn.getForeignKeyColumn();
                    if (foreignKeyColumn != null) {
                        DatasetEditorModelCell constraintCell = cell.getRow().getCellForColumn(constraintColumn);
                        if (constraintCell != null) {
                            Object value = constraintCell.getUserValue();
                            filterInput.setColumnValue(foreignKeyColumn, value);
                        }
                    }
                }
            }
            return filterInput;

        }
        return null;
    }

    /****************************************************************
     *                        Editor actions                        *
     ****************************************************************/
    public void deleteRecords(int[] rowIndexes) {
        DatasetEditorTable editorTable = getEditorTable();
        editorTable.fireEditingCancel();
        DBDataset dataset = getDataset();
        Progress.prompt(getProject(), dataset, true,
                "Deleting records",
                "Deleting records from " + dataset.getQualifiedNameWithType(),
                progress -> {
            progress.setIndeterminate(false);
            for (int index : rowIndexes) {
                progress.setFraction(Progress.progressOf(index, rowIndexes.length));
                DatasetEditorModelRow row = getRowAtIndex(index);
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
        DatasetEditorTable editorTable = getEditorTable();
        DBDataset dataset = getDataset();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            resultSetAdapter.startInsertRow();
            DatasetEditorModelRow newRow = createRow(getRowCount()+1);

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
        DatasetEditorTable editorTable = getEditorTable();
        DBDataset dataset = getDataset();
        try {
            set(INSERTING, true);
            editorTable.stopCellEditing();
            int insertIndex = rowIndex + 1;
            resultSetAdapter.startInsertRow();
            DatasetEditorModelRow oldRow = getRowAtIndex(rowIndex);
            DatasetEditorModelRow newRow = createRow(getRowCount() + 1);

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
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorModelRow row = getInsertRow();
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
            DatasetEditorError error = new DatasetEditorError(getConnection(), e);
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
        DatasetEditorTable editorTable = getEditorTable();
        try {
            editorTable.fireEditingCancel();
            DatasetEditorModelRow insertRow = getInsertRow();
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
        for (DatasetEditorModelRow row : getRows()) {
            if (row.getResultSetRowIndex() > fromIndex) {
                row.shiftResultSetRowIndex(shifting);
            }
        }
    }

    @Nullable
    public DatasetEditorModelRow getInsertRow() {
        for (DatasetEditorModelRow row : getRows()) {
            if (row.is(INSERTING)) {
                return row;
            }
        }
        return null;
    }

    public int getInsertRowIndex() {
        DatasetEditorModelRow insertRow = getInsertRow();
        return insertRow == null ? -1 : insertRow.getIndex();
    }

    public void revertChanges() {
        for (DatasetEditorModelRow row : getRows()) {
            row.revertChanges();
        }
        setModified(false);
    }

    public boolean isResultSetUpdatable() {
        return isResultSetUpdatable;
    }

    /*********************************************************
     *                      DataModel                       *
     *********************************************************/
    @Override
    public DatasetEditorModelCell getCellAt(int rowIndex, int columnIndex) {
        return super.getCellAt(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        DatasetEditorModelCell cell = getCellAt(rowIndex, columnIndex);
        if (cell == null) return;

        cell.updateUserValue(value, false);
    }

    public void setValueAt(Object value, String errorMessage,  int rowIndex, int columnIndex) {
        DatasetEditorModelCell cell = getCellAt(rowIndex, columnIndex);
        if (cell == null) return;

        cell.updateUserValue(value, errorMessage);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        DatasetEditorTable editorTable = getEditorTable();
        DatasetEditorState editorState = getState();
        if (isReadonly() || isEnvironmentReadonly() || isDirty()) return false;
        if (editorState.isReadonly()) return false;
        if (editorTable.isLoading()) return false;
        if (!editorTable.isEditingEnabled()) return false;
        if (editorTable.getSelectedColumnCount() > 1 || editorTable.getSelectedRowCount() > 1) return false;
        if (!getConnection().isConnected(SessionId.MAIN)) return false;

        DatasetEditorModelRow row = getRowAtIndex(rowIndex);
        if (row == null) return false;
        if (row.is(DELETED)) return false;
        if (row.is(UPDATING)) return false;
        if (is(INSERTING)) return row.is(INSERTING);

        DatasetEditorModelCell cell = row.getCellAtIndex(columnIndex);
        if (cell == null) return false;
        if (cell.is(UPDATING)) return false;

        return true;
    }

    public List<DBColumn> getUniqueKeyColumns() {
        return uniqueKeyColumns.get();
    }

    private List<DBColumn> loadUniqueKeyColumns() {
        DBTable table = (DBTable) getDataset();
        List<DBColumn> uniqueColumns = new ArrayList<>(table.getPrimaryKeyColumns());
        uniqueColumns.removeIf(c -> c.isIdentity());
        if (uniqueColumns.isEmpty()) {
            uniqueColumns = table.getUniqueKeyColumns();
        }

        return uniqueColumns;
    }

    /*********************************************************
     *                ListSelectionListener                  *
     *********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (!is(INSERTING)) return;
        if (event.getValueIsAdjusting()) return;

        DatasetEditorModelRow insertRow = getInsertRow();
        if (insertRow == null) return;

        int index = insertRow.getIndex();

        ListSelectionModel listSelectionModel = (ListSelectionModel) event.getSource();
        int selectionIndex = listSelectionModel.getLeadSelectionIndex();

        if (index != selectionIndex) {
            //postInsertRecord();
        }
    }
}
