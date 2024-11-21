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

import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.resultSet.ResultSetDataModelRow;
import com.dbn.editor.data.DatasetEditorError;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.DELETED;

public class DatasetEditorModelRow extends ResultSetDataModelRow<DatasetEditorModel, DatasetEditorModelCell> {

    public DatasetEditorModelRow(DatasetEditorModel model, ResultSet resultSet, int resultSetRowIndex) throws SQLException {
        super(model, resultSet, resultSetRowIndex);
    }

    @NotNull
    @Override
    public DatasetEditorModel getModel() {
        return super.getModel();
    }

    @Nullable
    DatasetEditorModelCell getCellForColumn(DBColumn column) {
        int columnIndex = getModel().getHeader().indexOfColumn(column);
        return getCellAtIndex(columnIndex);
    }

    @NotNull
    @Override
    protected DatasetEditorModelCell createCell(ResultSet resultSet, ColumnInfo columnInfo) throws SQLException {
        return new DatasetEditorModelCell(this, resultSet, (DatasetEditorColumnInfo) columnInfo);
    }

    void updateStatusFromRow(DatasetEditorModelRow oldRow) {
        if (oldRow == null) return;

        inherit(oldRow);
        setIndex(oldRow.getIndex());
        if (oldRow.isModified()) {
            for (int i=1; i<getCells().size(); i++) {
                DatasetEditorModelCell oldCell = oldRow.getCellAtIndex(i);
                DatasetEditorModelCell newCell = getCellAtIndex(i);
                if (oldCell != null && newCell != null) {
                    newCell.setOriginalUserValue(oldCell.getOriginalUserValue());
                }
            }
        }
    }

    void updateDataFromRow(DatasetEditorModelRow oldRow) {
        for (int i=0; i<getCells().size(); i++) {
            DatasetEditorModelCell oldCell = oldRow.getCellAtIndex(i);
            DatasetEditorModelCell newCell = getCellAtIndex(i);
            if (oldCell != null && newCell != null) {
                DatasetEditorColumnInfo columnInfo = oldCell.getColumnInfo();
                if (!columnInfo.isIdentity() && !columnInfo.isAuditColumn()) {
                    newCell.updateUserValue(oldCell.getUserValue(), false);
                }
            }
        }
    }

    public void delete() {
        try {
            ResultSetAdapter resultSetAdapter = getModel().getResultSetAdapter();
            resultSetAdapter.scroll(getResultSetRowIndex());
            resultSetAdapter.deleteRow();

            reset();
            set(DELETED, true);
        } catch (SQLException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), "Cannot delete record", "Could not delete row at index " + getIndex() + ".\nCause: " + e.getMessage());
        }
    }

    public boolean matches(DataModelRow row, boolean lenient) {
        // try fast match by primary key
        DatasetEditorModel model = getModel();
        if (model.getDataset() instanceof DBTable) {
            List<DBColumn> uniqueKeyColumns = model.getUniqueKeyColumns();
            if (!uniqueKeyColumns.isEmpty()) {
                for (DBColumn uniqueColumn : uniqueKeyColumns) {
                    int index = model.getHeader().indexOfColumn(uniqueColumn);
                    DatasetEditorModelCell localCell = getCellAtIndex(index);
                    DatasetEditorModelCell remoteCell = (DatasetEditorModelCell) row.getCellAtIndex(index);
                    if (localCell != null && remoteCell != null) {
                        if (!localCell.matches(remoteCell, false)) return false;
                    }
                }
                return true;
            }
        }

        // try to match all columns
        for (int i=0; i<getCells().size(); i++) {
            DatasetEditorModelCell localCell = getCellAtIndex(i);
            DatasetEditorModelCell remoteCell = (DatasetEditorModelCell) row.getCellAtIndex(i);

            //local cell is usually the cell on client side.
            // remote cell may have been changed by a trigger on update/insert
            /*if (!localCell.equals(remoteCell) && (localCell.getUserValue()!= null || !ignoreNulls)) {
                return false;
            }*/
            if (localCell != null && remoteCell != null) {
                DatasetEditorColumnInfo columnInfo = localCell.getColumnInfo();
                if (!columnInfo.isAuditColumn() && !localCell.matches(remoteCell, lenient)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void notifyError(DatasetEditorError error, boolean startEditing, boolean showPopup) {
        checkDisposed();

        DBObject messageObject = error.getMessageObject();
        if (messageObject == null) return;

        if (messageObject instanceof DBColumn) {
            DBColumn column = (DBColumn) messageObject;
            DatasetEditorModelCell cell = getCellForColumn(column);
            if (cell != null) {
                boolean isErrorNew = cell.notifyError(error, true);
                if (isErrorNew && startEditing) cell.edit();
            }
        } else if (messageObject instanceof DBConstraint) {
            DBConstraint constraint = (DBConstraint) messageObject;
            DatasetEditorModelCell firstCell = null;
            boolean isErrorNew = false;
            for (DBColumn column : constraint.getColumns()) {
                DatasetEditorModelCell cell = getCellForColumn(column);
                if (cell != null) {
                    isErrorNew = cell.notifyError(error, false);
                    if (firstCell == null) firstCell = cell;
                }
            }
            if (isErrorNew && showPopup) {
                DatasetEditorTable table = getModel().getEditorTable();
                table.showErrorPopup(firstCell);
                error.setNotified(true);
            }
        }
    }

    public void revertChanges() {
        if (!isModified()) return;

        for (DatasetEditorModelCell cell : getCells()) {
            cell.revertChanges();
        }
        setModified(false);
    }


    @Override
    public int getResultSetRowIndex() {
        return is(DELETED) ? -1 : super.getResultSetRowIndex();
    }

    @Override
    public void shiftResultSetRowIndex(int delta) {
        assert isNot(DELETED);
        super.shiftResultSetRowIndex(delta);
    }

    @NotNull
    ConnectionHandler getConnectionHandler() {
        return getModel().getConnection();
    }

    public boolean isResultSetUpdatable() {
        return getModel().isResultSetUpdatable();
    }

    public boolean isEmptyData() {
        for (DatasetEditorModelCell cell : getCells()) {
            Object userValue = cell.getUserValue();
            if (userValue == null) continue;

            if (userValue instanceof String) {
                String stringUserValue = (String) userValue;
                if (Strings.isNotEmpty(stringUserValue)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
