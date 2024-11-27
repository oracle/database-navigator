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

import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.Resources;
import com.dbn.connection.Savepoints;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.type.DBDataType;
import com.dbn.data.value.ValueAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

public class EditableResultSetAdapter extends ResultSetAdapter {
    private DBNResultSet resultSet;

    public EditableResultSetAdapter(DatasetEditorModel model, DBNResultSet resultSet) {
        super(model);
        this.resultSet = resultSet;
    }

    @Override
    public void scroll(int rowIndex) throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> absolute(resultSet, rowIndex));
        } else {
            absolute(resultSet, rowIndex);
        }
    }

    @Override
    public void updateRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> updateRow(resultSet));
        } else {
            updateRow(resultSet);
        }
    }

    @Override
    public void refreshRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> refreshRow(resultSet));
        } else {
            refreshRow(resultSet);
        }
    }

    @Override
    public void startInsertRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> {
                moveToInsertRow(resultSet);
                setInsertMode(true);
            });
        } else {
            moveToInsertRow(resultSet);
            setInsertMode(true);
        }
    }

    @Override
    public void cancelInsertRow() throws SQLException {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> {
                moveToCurrentRow(resultSet);
                setInsertMode(false);
            });
        } else {
            moveToCurrentRow(resultSet);
            setInsertMode(false);
        }
    }

    @Override
    public void insertRow() throws SQLException {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> {
                insertRow(resultSet);
                moveToCurrentRow(resultSet);
                setInsertMode(false);
            });
        } else {
            insertRow(resultSet);
            moveToCurrentRow(resultSet);
            setInsertMode(false);
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet, () -> deleteRow(resultSet));
        } else {
            deleteRow(resultSet);
        }
    }

    private boolean isObsolete() {
        return Resources.isObsolete(resultSet);
    }

    @Override
    public void setValue(int columnIndex, @NotNull ValueAdapter valueAdapter, @Nullable Object value) throws SQLException {
        DBNResultSet resultSet = getResultSet();
        Connection connection = resultSet.getConnection();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet,
                    () -> valueAdapter.write(connection, resultSet, columnIndex, value));
        } else {
            valueAdapter.write(connection, resultSet, columnIndex, value);
        }
    }

    @Override
    public void setValue(int columnIndex, @NotNull DBDataType dataType, @Nullable Object value) throws SQLException {
        DBNResultSet resultSet = getResultSet();
        if (isUseSavePoints()) {
            Savepoints.run(resultSet,
                    () -> dataType.setValueToResultSet(resultSet, columnIndex, value));
        } else {
            dataType.setValueToResultSet(resultSet, columnIndex, value);
        }
    }

    @NotNull
    DBNResultSet getResultSet() {
        return Failsafe.nn(resultSet);
    }

    @Override
    public void disposeInner() {
        resultSet = null;
        super.disposeInner();
    }
}
