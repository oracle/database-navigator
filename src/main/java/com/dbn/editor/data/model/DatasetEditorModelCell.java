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


import com.dbn.common.event.ProjectEvents;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.value.ValueAdapter;
import com.dbn.editor.data.DatasetEditorError;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Rectangle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatasetEditorModelCell
        extends ResultSetDataModelCell<DatasetEditorModelRow, DatasetEditorModel>
        implements ChangeListener {

    private static final WeakRefCache<DatasetEditorModelCell, Object> originalUserValues = WeakRefCache.weakKey();
    private static final WeakRefCache<DatasetEditorModelCell, String> temporaryUserValues = WeakRefCache.weakKey();
    private static final WeakRefCache<DatasetEditorModelCell, DatasetEditorError> errors = WeakRefCache.weakKey();
    private static final Object NULL = new Object();

    public DatasetEditorModelCell(DatasetEditorModelRow row, ResultSet resultSet, DatasetEditorColumnInfo columnInfo) throws SQLException {
        super(row, resultSet, columnInfo);
    }

    @Override
    public DatasetEditorColumnInfo getColumnInfo() {
        return (DatasetEditorColumnInfo) super.getColumnInfo();
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
        DatasetEditorColumnInfo columnInfo = getColumnInfo();
        GenericDataType genericDataType = columnInfo.getDataType().getGenericDataType();
        boolean isValueAdapter = ValueAdapter.supports(genericDataType);

        if (!isValueAdapter && valueChanged) {
            setUserValue(newUserValue);
        }

        Project project = getProject();
        DatasetEditorModelRow row = getRow();
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

            if (isValueAdapter) {
                ValueAdapter<?> valueAdapter = ValueAdapter.create(genericDataType);
                if (valueAdapter != null) {
                    if (newUserValue instanceof ValueAdapter) {
                        ValueAdapter<?> newValueAdapter = (ValueAdapter<?>) newUserValue;
                        newUserValue = newValueAdapter.read();
                    }
                    resultSetAdapter.setValue(columnIndex, valueAdapter, newUserValue);
                }
                setUserValue(valueAdapter);
            } else {
                DBDataType dataType = columnInfo.getDataType();
                resultSetAdapter.setValue(columnIndex, dataType, newUserValue);
            }


            resultSetAdapter.updateRow();
        } catch (Exception e) {
            conditionallyLog(e);
            //try { Thread.sleep(6000); } catch (InterruptedException e1) { e1.printStackTrace(); }

            DatasetEditorError error = new DatasetEditorError(connection, e);

            // error may affect other cells in the row (e.g. foreign key constraint for multiple primary key)
            if (e instanceof SQLException) {
                row.notifyError(error, false, !bulk);
            }

            // if error was not notified yet on row level, notify it on cell isolation level
            if (!error.isNotified()) notifyError(error, !bulk);
        } finally {
            if (valueChanged) {
                DBNConnection conn = getResultConnection();
                conn.notifyDataChanges(getDataset().getVirtualFile());
                ProjectEvents.notify(project,
                        DatasetEditorModelCellValueListener.TOPIC,
                        (listener) -> listener.valueChanged(this));
            }
            try {
                resultSetAdapter.refreshRow();
            } catch (SQLException e) {
                conditionallyLog(e);
                DatasetEditorError error = new DatasetEditorError(connection, e);
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

    protected DBDataset getDataset() {
        return getEditorModel().getDataset();
    }

    private boolean userValueChanged(Object newUserValue) {
        Object userValue = getUserValue();
        if (userValue instanceof ValueAdapter) {
            ValueAdapter<?> valueAdapter = (ValueAdapter<?>) userValue;
            try {
                return !Commons.match(valueAdapter.read(), newUserValue);
            } catch (SQLException e) {
                conditionallyLog(e);
                return true;
            }
        }

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
            DatasetEditorModelRow row = getRow();
            DatasetEditorError error = new DatasetEditorError(errorMessage, getColumn());
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

    public boolean matches(DatasetEditorModelCell remoteCell, boolean lenient) {
        if (Commons.match(getUserValue(), remoteCell.getUserValue())){
            return true;
        }
        DatasetEditorModelRow row = getRow();
        if (lenient && (row.is(RecordStatus.INSERTED) || row.isModified()) && getUserValue() == null && remoteCell.getUserValue() != null) {
            return true;
        }
        return false;
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return getEditorModel().getConnection();
    }

    private DatasetEditorTable getEditorTable() {
        return getEditorModel().getEditorTable();
    }

    @NotNull
    private DatasetEditorModel getEditorModel() {
        return getRow().getModel();
    }

    private void checkColumnBounds(int index) {
        getModel().checkColumnBounds(index);
    }

    private int getViewColumnIndex() {
        return getEditorTable().convertColumnIndexToView(getIndex());
    }

    public void edit() {
        Dispatch.run(true, () -> {
            int index = getViewColumnIndex();
            checkColumnBounds(index);

            DatasetEditorTable table = getEditorTable();
            table.editCellAt(getRow().getIndex(), index);
        });
    }

    public void editPrevious() {
        Dispatch.run(true, () -> {
            int index = getViewColumnIndex();
            checkColumnBounds(index);

            DatasetEditorTable table = getEditorTable();
            DatasetEditorModelRow row = getRow();
            table.editCellAt(row.getIndex(), index -1);
        });
    }

    public void editNext(){
        Dispatch.run(true, () -> {
            int index = getViewColumnIndex();
            checkColumnBounds(index);

            DatasetEditorModelRow row = getRow();
            if (index < row.getCells().size()-1) {
                DatasetEditorTable table = getEditorTable();
                table.editCellAt(row.getIndex(), index + 1);
            }
        });
    }

    @Override
    @NotNull
    public DatasetEditorModelRow getRow() {
        return super.getRow();
    }

    @NotNull
    @Override
    public DatasetEditorModel getModel() {
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

    public boolean isEditing() {
        DatasetEditorTable table = getEditorTable();
        return table.isEditing() &&
               table.isCellSelected(getRow().getIndex(), getIndex());
    }

    public boolean isNavigable() {
        DBColumn column = getColumn();
        return column.isForeignKey() && getUserValue() != null;
    }

    private void notifyCellUpdated() {
        getEditorModel().notifyCellUpdated(getRow().getIndex(), getIndex());
    }

    private void scrollToVisible() {
        Dispatch.run(() -> {
            int rowIndex = getRow().getIndex();
            int colIndex = getIndex();
            DatasetEditorTable table = getEditorTable();
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
        DatasetEditorError error = getError();
        if (error != null && error.isDirty()) {
            error = null;
        }
        return error != null;
    }

    boolean notifyError(DatasetEditorError error, boolean showPopup) {
        error.setNotified(true);
        if (Commons.match(getError(), error, err -> err.getMessage())) return false;

        clearError();
        setError(error);
        notifyCellUpdated();
        if (showPopup) {
            scrollToVisible();
        }

        DatasetEditorTable table = getEditorTable();
        if (isEditing()) {
            DatasetTableCellEditor cellEditor = table.getCellEditor();
            if (cellEditor != null) {
                cellEditor.highlight(DatasetTableCellEditor.HIGHLIGHT_TYPE_ERROR);
            }
        }
        error.addChangeListener(this);
        if (showPopup) {
            table.showErrorPopup(this);
        }
        return true;
    }

    private void clearError() {
        DatasetEditorError error = getError();
        if (error == null) return;

        error.markDirty();
        setError(null);
    }

    public DatasetEditorError getError() {
        return errors.get(this);
    }

    private void setError(DatasetEditorError error) {
        if (error == null) errors.remove(this); else errors.set(this, error);
    }

    public void revertChanges() {
        if (!isModified()) return;

        updateUserValue(getOriginalUserValue(), false);
        setModified(false);
    }

    public DBColumn getColumn() {
        return getColumnInfo().getColumn();
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        temporaryUserValues.remove(this);
        originalUserValues.remove(this);
        errors.remove(this);
    }
}
