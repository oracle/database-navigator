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


import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Json;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.data.model.resultSet.ResultSetColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dbn.data.value.JsonValue;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class JsonDataEditorModelCell
        extends ResultSetDataModelCell<JsonDataEditorModelRow, JsonDataEditorModel>
        implements ChangeListener {

    private static final WeakRefCache<JsonDataEditorModelCell, JsonValue> originalUserValues = WeakRefCache.weakKey();
    private static final WeakRefCache<JsonDataEditorModelCell, JsonDataEditorError> errors = WeakRefCache.weakKey();

    private static final JsonValue NULL = new JsonValue(); // surrogate value to store nulls as original value

    public JsonDataEditorModelCell(JsonDataEditorModelRow row, ResultSet resultSet, ResultSetColumnInfo columnInfo) throws SQLException {
        super(row, resultSet, columnInfo);
    }

    @Override
    public ResultSetColumnInfo getColumnInfo() {
        return (ResultSetColumnInfo) super.getColumnInfo();
    }

    @Override
    public void updateUserValue(Object newUserValue, boolean bulk) {
        Background.run(() -> {
            try {
                set(RecordStatus.UPDATING, true);
                updateValue((JsonValue) newUserValue);
                notifyCellUpdated();
            } finally {
                set(RecordStatus.UPDATING, false);
            }
        });
    }

    @Override
    public String getPresentableValue() {
        boolean presentable = getEditorTable().getEditor().isContentEditorVisible();

        return presentable ?
                super.getPresentableValue() :
                getJsonContent();
    }

    @Override
    public JsonValue getUserValue() {
        return (JsonValue) super.getUserValue();
    }

    private void updateValue(JsonValue newUserValue) {
        ConnectionHandler connection = getConnection();
        connection.updateLastAccess();

        initOriginalValue();
        setUserValue(newUserValue);

        Project project = getProject();
        JsonDataEditorModelRow row = getRow();
        ResultSetAdapter resultSetAdapter = getModel().getResultSetAdapter();
        try {
            resultSetAdapter.scroll(row.getResultSetRowIndex());
        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(project, txt("msg.dataEditor.error.FailedToUpdateJsonRecord"), e);
            return;
        }

        try {
            clearError();
            resultSetAdapter.updateRow();
        } catch (Exception e) {
            conditionallyLog(e);
            JsonDataEditorError error = new JsonDataEditorError(connection, e);
            notifyError(error, true);
        } finally {
            DBNConnection conn = getResultConnection();
            conn.notifyDataChanges(getJsonView().getVirtualFile());
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

        JsonValue value = getUserValue();
        originalUserValues.set(this, value == null ? NULL : value);
    }

    protected DBJsonView getJsonView() {
        return getEditorModel().getJsonView();
    }

    public boolean matches(JsonDataEditorModelCell remoteCell) {
        Map<String, Object> identityAttributes = getIdentityAttributes();
        Map<String, Object> remoteIdentityAttributes = remoteCell.getIdentityAttributes();

        return (Objects.equals(identityAttributes, remoteIdentityAttributes));
    }

    private Map<String, Object> getIdentityAttributes() {
        List<String> keyAttributes = getJsonView().getKeyAttributeNames();
        return Json.getJsonPropertyValues(getUserValue().getData(), keyAttributes);
    }

    @Override
    protected String createPresentableValue() {
        Object userValue = getUserValue();
        if (userValue == null) return "";
        String stringValue = Objects.toString(userValue);
        return Json.createJsonPreview(stringValue, 2);
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

    public JsonValue getOriginalUserValue() {
        JsonValue value = originalUserValues.get(this);
        return value == NULL ? null : value;
    }

    void setOriginalUserValue(JsonValue value) {
        Object originalUserValue = getOriginalUserValue();

        if (originalUserValue == null) {
            setModified(value != null);
        } else {
            setModified(!originalUserValue.equals(value));
        }

        originalUserValues.set(this, value == null ? NULL : value);
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

        setUserValue(getOriginalUserValue());
        setModified(false);
    }

    public String getJsonContent() {
        return getUserValue().getData();
    }

    public String getOriginalJsonContent() {
        JsonValue userValue = nvl(getOriginalUserValue(), getUserValue());
        return userValue.getData();
    }


    @Override
    public void disposeInner() {
        super.disposeInner();
        originalUserValues.remove(this);
        errors.remove(this);
    }

}
