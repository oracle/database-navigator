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
import com.dbn.common.util.Lists;
import com.dbn.connection.Resources;
import com.dbn.connection.Savepoints;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.value.ValueAdapter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReadonlyResultSetAdapter extends ResultSetAdapter {
    private DBNConnection connection;
    private Row currentRow;

    ReadonlyResultSetAdapter(DatasetEditorModel model, DBNResultSet resultSet) {
        super(model);
        this.connection = resultSet.getConnection();
    }

    @Override
    public synchronized void scroll(final int rowIndex) throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        DatasetEditorModelRow modelRow = getModel().getRowAtResultSetIndex(rowIndex);
        if (modelRow == null) {
            throw new SQLException("Could not scroll to row index " + rowIndex);
        }

        currentRow = new Row();
        List<DatasetEditorModelCell> modelCells = modelRow.getCells();
        for (DatasetEditorModelCell modelCell : modelCells) {
            DatasetEditorColumnInfo columnInfo = modelCell.getColumnInfo();
            if (columnInfo.isPrimaryKey()) {
                currentRow.addKeyCell(columnInfo, modelCell.getUserValue());
            }
        }
    }

    @Override
    public synchronized void updateRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> this.executeUpdate());
        } else {
            executeUpdate();
        }
    }

    @Override
    public synchronized void refreshRow() {
        // not supported
    }


    @Override
    public synchronized void startInsertRow() {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        setInsertMode(true);
        currentRow = new Row();
    }

    @Override
    public synchronized void cancelInsertRow() {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        setInsertMode(false);
        currentRow = null;
    }

    @Override
    public synchronized void insertRow() throws SQLException {
        if (!isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> {
                executeInsert();
                setInsertMode(false);
            });
        } else {
            executeInsert();
            setInsertMode(false);
        }
    }

    @Override
    public synchronized void deleteRow() throws SQLException {
        if (isInsertMode()) return;
        if (isObsolete()) return;

        if (isUseSavePoints()) {
            Savepoints.run(connection, () -> executeDelete());
        } else {
            executeDelete();
        }
    }

    private boolean isObsolete() {
        return Resources.isObsolete(connection);
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final ValueAdapter valueAdapter, @Nullable final Object value) {
        DatasetEditorColumnInfo columnInfo = getColumnInfo(columnIndex);
        currentRow.addChangedCell(columnInfo, value);
    }

    @Override
    public synchronized void setValue(final int columnIndex, @NotNull final DBDataType dataType, @Nullable final Object value) {
        DatasetEditorColumnInfo columnInfo = getColumnInfo(columnIndex);
        currentRow.addChangedCell(columnInfo, value);
    }

    DatasetEditorColumnInfo getColumnInfo(int columnIndex) {
        return getModel().getHeader().getResultSetColumnInfo(columnIndex);
    }

    private void executeUpdate() throws SQLException {
        List<Cell> keyCells = currentRow.getKeyCells();
        if (keyCells.isEmpty()) {
            throw new SQLException("No primary key defined for table");
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("update ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" set ");

        List<Cell> changedCells = currentRow.getChangedCells();
        for (Cell cell : changedCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ?");
            if (!Lists.isLast(changedCells, cell)) {
                buffer.append(", ");
            }
        }
        buffer.append(" where ");

        for (Cell cell : keyCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ?");
            if (!Lists.isLast(keyCells, cell)) {
                buffer.append(" and ");
            }
        }

        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : changedCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        for (Cell cell : keyCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private void executeInsert() throws SQLException {
        List<Cell> changedCells = currentRow.getChangedCells();
        if (changedCells.isEmpty()) {
            throw AlreadyDisposedException.INSTANCE;
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("insert into ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" (");

        for (Cell cell : changedCells) {
            buffer.append(cell.getColumnName());
            buffer.append(", ");
        }
        buffer.delete(buffer.length() -2, buffer.length());
        buffer.append(" ) values (");

        for (Cell cell : changedCells) {
            buffer.append(" ? ");
            buffer.append(", ");
        }
        buffer.delete(buffer.length() -2, buffer.length());
        buffer.append(")");


        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : changedCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    private void executeDelete() throws SQLException {
        List<Cell> keyCells = currentRow.getKeyCells();
        if (keyCells.size() == 0) {
            throw new SQLException("No primary key defined for table");
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("delete from ");
        buffer.append(getModel().getDataset().getQualifiedName());
        buffer.append(" where ");

        for (Cell cell : keyCells) {
            buffer.append(cell.getColumnName());
            buffer.append(" = ? ");
        }

        PreparedStatement preparedStatement = connection.prepareStatement(buffer.toString());
        int paramIndex = 0;
        for (Cell cell : keyCells) {
            paramIndex++;
            DBNativeDataType nativeDataType = cell.getDataType();
            nativeDataType.setValueToStatement(preparedStatement, paramIndex, cell.getValue());
        }
        preparedStatement.executeUpdate();
    }

    @Getter
    @EqualsAndHashCode
    private static class Cell {
        private final ColumnInfo columnInfo;
        private transient final Object value;

        Cell(ColumnInfo columnInfo, Object value) {
            this.columnInfo = columnInfo;
            this.value = value;
        }

        @NotNull
        public DBNativeDataType getDataType() throws SQLException {
            DBDataType dataType = columnInfo.getDataType();
            DBNativeDataType nativeDataType = dataType.getNativeType();
            if (nativeDataType == null) {
                throw new SQLException("Operation not supported for " + dataType.getName());
            }
            return nativeDataType;
        }

        public String getColumnName() {
            return columnInfo.getName();
        }
    }

    private static class Row {
        private final Set<Cell> keyCells = new HashSet<>();
        private final Set<Cell> changedCells = new HashSet<>();

        List<Cell> getKeyCells() {
            return new ArrayList<>(keyCells);
        }

        List<Cell> getChangedCells() {
            return new ArrayList<>(changedCells);
        }

        void addKeyCell(ColumnInfo columnInfo, Object value) {
            Cell cell = new Cell(columnInfo, value);
            keyCells.remove(cell);
            keyCells.add(cell);
        }

        void addChangedCell(ColumnInfo columnInfo, Object value) {
            Cell cell = new Cell(columnInfo, value);
            changedCells.remove(cell);
            changedCells.add(cell);
        }
    }

    @Override
    public void disposeInner() {
        currentRow = null;
        connection = null;
        super.disposeInner();
    }
}
