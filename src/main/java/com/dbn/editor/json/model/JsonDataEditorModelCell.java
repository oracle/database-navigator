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


import com.dbn.common.locale.Formatter;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.data.model.ResultSetAdapter;
import com.dbn.editor.json.ui.JsonDataEditorError;
import com.dbn.editor.json.ui.table.JsonDataEditorTable;
import com.dbn.object.DBJsonView;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Rectangle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class JsonDataEditorModelCell
        extends ResultSetDataModelCell<JsonDataEditorModelRow, JsonDataEditorModel>
        implements ChangeListener {

    private static final WeakRefCache<JsonDataEditorModelCell, Object> originalUserValues = WeakRefCache.weakKey();
    private static final WeakRefCache<JsonDataEditorModelCell, String> temporaryUserValues = WeakRefCache.weakKey();
    private static final WeakRefCache<JsonDataEditorModelCell, JsonDataEditorError> errors = WeakRefCache.weakKey();
    private static final Object NULL = new Object();

    public JsonDataEditorModelCell(JsonDataEditorModelRow row, ResultSet resultSet, ResultSetColumnInfo columnInfo) throws SQLException {
        super(row, resultSet, columnInfo);
    }

    @Override
    public ResultSetColumnInfo getColumnInfo() {
        return (ResultSetColumnInfo) super.getColumnInfo();
    }

    @Override
    public void updateUserValue(Object newUserValue, boolean bulk) {
        try {
            set(RecordStatus.UPDATING, true);
            updateValue(newUserValue, bulk);
        } finally {
            setTemporaryUserValue(null);
            set(RecordStatus.UPDATING, false);
        }
    }

    private void updateValue(Object newUserValue, boolean bulk) {
        ConnectionHandler connection = getConnection();
        connection.updateLastAccess();

        boolean valueChanged = userValueChanged(newUserValue);
        if (!valueChanged && !hasError()) return;

        initOriginalValue();
        ResultSetColumnInfo columnInfo = getColumnInfo();

        if (valueChanged) {
            setUserValue(newUserValue);
        }

        Project project = getProject();
        JsonDataEditorModelRow row = getRow();
        ResultSetAdapter resultSetAdapter = getModel().getResultSetAdapter();
        try {
            resultSetAdapter.scroll(row.getResultSetRowIndex());
        } catch (Exception e) {
            conditionallyLog(e);
            Messages.showErrorDialog(project, txt("msg.dataEditor.error.FailedToUpdateCell",  columnInfo.getName()), e);
            return;
        }

        try {
            clearError();
            int columnIndex = columnInfo.getResultSetIndex();
            DBDataType dataType = columnInfo.getDataType();
            resultSetAdapter.setValue(columnIndex, dataType, newUserValue);
            resultSetAdapter.updateRow();
        } catch (Exception e) {
            conditionallyLog(e);
            //try { Thread.sleep(6000); } catch (InterruptedException e1) { e1.printStackTrace(); }

            JsonDataEditorError error = new JsonDataEditorError(connection, e);

            // error may affect other cells in the row (e.g. foreign key constraint for multiple primary key)
            if (e instanceof SQLException) {
                row.notifyError(error, false, !bulk);
            }

            // if error was not notified yet on row level, notify it on cell isolation level
            if (!error.isNotified()) notifyError(error, !bulk);
        } finally {
            if (valueChanged) {
                DBNConnection conn = getResultConnection();
                conn.notifyDataChanges(getJsonView().getVirtualFile());
            }
            try {
                resultSetAdapter.refreshRow();
            } catch (SQLException e) {
                conditionallyLog(e);
                JsonDataEditorError error = new JsonDataEditorError(connection, e);
                row.notifyError(error, false, !bulk);
            }
        }

        if (row.isNot(RecordStatus.INSERTING) && !connection.isAutoCommit()) {
            reset();
            setModified(true);

            row.reset();
            row.setModified(true);
            row.getModel().setModified(true);
        }
    }

    private void initOriginalValue() {
        if (originalUserValues.contains(this)) return;

        Object value = getUserValue();
        originalUserValues.set(this, value == null ? NULL : value);
    }

    protected DBJsonView getJsonView() {
        return getEditorModel().getJsonView();
    }

    private boolean userValueChanged(Object newUserValue) {
        Object userValue = getUserValue();

        if (userValue != null && newUserValue != null) {
            if (userValue.equals(newUserValue)) {
                return false;
            }
            // user input may not contain the entire precision (e.g. date time format)
            Formatter formatter = getFormatter();
            String formattedValue1 = formatter.formatObject(userValue);
            String formattedValue2 = formatter.formatObject(newUserValue);
            return !Objects.equals(formattedValue1, formattedValue2);
        }
        
        return !Commons.match(userValue, newUserValue);
    }

    public void updateUserValue(Object userValue, String errorMessage) {
        ConnectionHandler connection = getConnection();
        connection.updateLastAccess();

        if (!Commons.match(userValue, getUserValue()) || hasError()) {
            JsonDataEditorModelRow row = getRow();
            JsonDataEditorError error = new JsonDataEditorError(errorMessage, null);
            getRow().notifyError(error, true, true);
            setUserValue(userValue);
            if (row.isNot(RecordStatus.INSERTING) && !connection.isAutoCommit()) {
                reset();
                setModified(true);

                row.reset();
                row.setModified(true);
                row.getModel().setModified(true);
            }
        }
    }

    public boolean matches(JsonDataEditorModelCell remoteCell, boolean lenient) {
        if (Commons.match(getUserValue(), remoteCell.getUserValue())){
            return true;
        }
        JsonDataEditorModelRow row = getRow();
        if (lenient && (row.is(RecordStatus.INSERTED) || row.isModified()) && getUserValue() == null && remoteCell.getUserValue() != null) {
            return true;
        }
        return false;
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return getEditorModel().getConnection();
    }

    private JsonDataEditorTable getEditorTable() {
        return getEditorModel().getEditorTable();
    }

    @NotNull
    private JsonDataEditorModel getEditorModel() {
        return getRow().getModel();
    }

    @Override
    @NotNull
    public JsonDataEditorModelRow getRow() {
        return super.getRow();
    }

    @NotNull
    @Override
    public JsonDataEditorModel getModel() {
        return super.getModel();
    }

    public Object getOriginalUserValue() {
        Object value = originalUserValues.get(this);
        return value == NULL ? null : value;
    }

    void setOriginalUserValue(Object value) {
        Object originalUserValue = getOriginalUserValue();

        if (originalUserValue == null) {
            setModified(value != null);
        } else {
            setModified(!originalUserValue.equals(value));
        }

        originalUserValues.set(this, value == null ? NULL : value);
    }

    public String getTemporaryUserValue() {
        return temporaryUserValues.get(this);
    }

    public void setTemporaryUserValue(String temporaryUserValue) {
        if (temporaryUserValue == null)
            temporaryUserValues.remove(this); else
            temporaryUserValues.set(this, temporaryUserValue);
    }

    private void notifyCellUpdated() {
        getEditorModel().notifyCellUpdated(getRow().getIndex(), getIndex());
    }

    private void scrollToVisible() {
        Dispatch.run(getEditorTable(), () -> {
            int rowIndex = getRow().getIndex();
            int colIndex = getIndex();
            JsonDataEditorTable table = getEditorTable();
            Rectangle cellRect = table.getCellRect(rowIndex, colIndex, true);
            table.scrollRectToVisible(cellRect);
        });
    }

    public boolean isResultSetUpdatable() {
        return getRow().isResultSetUpdatable();
    }

    /*********************************************************
     *                    ChangeListener                     *
     *********************************************************/
    @Override
    public void stateChanged(ChangeEvent e) {
        notifyCellUpdated();
    }


    /*********************************************************
     *                        ERROR                          *
     *********************************************************/
    public boolean hasError() {
        JsonDataEditorError error = getError();
        if (error != null && error.isDirty()) {
            error = null;
        }
        return error != null;
    }

    boolean notifyError(JsonDataEditorError error, boolean showPopup) {
        error.setNotified(true);
        if (Commons.match(getError(), error, err -> err.getMessage())) return false;

        clearError();
        setError(error);
        notifyCellUpdated();
        if (showPopup) {
            scrollToVisible();
        }

        JsonDataEditorTable table = getEditorTable();
        error.addChangeListener(this);
        if (showPopup) {
            table.showErrorPopup(this);
        }
        return true;
    }

    private void clearError() {
        JsonDataEditorError error = getError();
        if (error == null) return;

        error.markDirty();
        setError(null);
    }

    public JsonDataEditorError getError() {
        return errors.get(this);
    }

    private void setError(JsonDataEditorError error) {
        if (error == null) errors.remove(this); else errors.set(this, error);
    }

    public void revertChanges() {
        if (!isModified()) return;

        updateUserValue(getOriginalUserValue(), false);
        setModified(false);
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        temporaryUserValues.remove(this);
        originalUserValues.remove(this);
        errors.remove(this);
    }
}
