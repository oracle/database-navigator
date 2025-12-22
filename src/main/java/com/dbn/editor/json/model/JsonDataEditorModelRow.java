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

import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelRow;
import com.dbn.editor.data.model.ResultSetAdapter;
import com.dbn.editor.json.ui.JsonDataEditorError;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.DELETED;

public class JsonDataEditorModelRow extends ResultSetDataModelRow<JsonDataEditorModel, JsonDataEditorModelCell> {

    public JsonDataEditorModelRow(JsonDataEditorModel model, ResultSet resultSet, int resultSetRowIndex) throws SQLException {
        super(model, resultSet, resultSetRowIndex);
    }

    @NotNull
    @Override
    public JsonDataEditorModel getModel() {
        return super.getModel();
    }

    @NotNull
    @Override
    protected JsonDataEditorModelCell createCell(ResultSet resultSet, ColumnInfo columnInfo) throws SQLException {
        return new JsonDataEditorModelCell(this, resultSet, (ResultSetColumnInfo) columnInfo);
    }

    void updateStatusFromRow(JsonDataEditorModelRow oldRow) {
        if (oldRow == null) return;

        inherit(oldRow);
        setIndex(oldRow.getIndex());
        if (oldRow.isModified()) {
            for (int i=1; i<getCells().size(); i++) {
                JsonDataEditorModelCell oldCell = oldRow.getCellAtIndex(i);
                JsonDataEditorModelCell newCell = getCellAtIndex(i);
                if (oldCell != null && newCell != null) {
                    newCell.setOriginalUserValue(oldCell.getOriginalUserValue());
                }
            }
        }
    }

    void updateDataFromRow(JsonDataEditorModelRow oldRow) {
        for (int i=0; i<getCells().size(); i++) {
            JsonDataEditorModelCell oldCell = oldRow.getCellAtIndex(i);
            JsonDataEditorModelCell newCell = getCellAtIndex(i);
            if (oldCell != null && newCell != null) {
                newCell.updateUserValue(oldCell.getUserValue(), false);
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
            Messages.showErrorDialog(getProject(),
                    txt("msg.dataEditor.title.CannotDeleteRecord"),
                    txt("msg.dataEditor.error.CannotDeleteRecord",  getIndex(), e.getMessage()));
        }
    }

    public boolean matches(DataModelRow row) {
        // try fast match by primary key
        JsonDataEditorModelCell localCell = getCellAtIndex(0);
        JsonDataEditorModelCell remoteCell = (JsonDataEditorModelCell) row.getCellAtIndex(0);
        return localCell != null &&
                remoteCell != null &&
                localCell.matches(remoteCell);
    }

    public void notifyError(JsonDataEditorError error, boolean startEditing, boolean showPopup) {
        checkDisposed();

/*        DBObject messageObject = error.getMessageObject();
        if (messageObject == null) return;

        if (messageObject instanceof DBColumn) {
            DBColumn column = (DBColumn) messageObject;
            JsonDataEditorModelCell cell = getCellForColumn(column);
            if (cell != null) {
                boolean isErrorNew = cell.notifyError(error, true);
                if (isErrorNew && startEditing) cell.edit();
            }
        } else if (messageObject instanceof DBConstraint) {
            DBConstraint constraint = (DBConstraint) messageObject;
            JsonDataEditorModelCell firstCell = null;
            boolean isErrorNew = false;
            for (DBColumn column : constraint.getColumns()) {
                JsonDataEditorModelCell cell = getCellForColumn(column);
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
        }*/
    }

    public void revertChanges() {
        if (!isModified()) return;

        for (JsonDataEditorModelCell cell : getCells()) {
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
        for (JsonDataEditorModelCell cell : getCells()) {
            Object userValue = cell.getUserValue();
            if (userValue == null) continue;

            if (userValue instanceof String stringUserValue) {
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
