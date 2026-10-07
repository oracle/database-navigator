/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.data.value;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBNestedTable;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static com.dbn.data.type.GenericDataType.TABLE;

@Getter
public class NestedTableValue extends LargeObjectValue {
    public static final String DISPLAY_VALUE = "[TABLE]";

    private final DBObjectRef<DBNestedTable> nestedTable;
    private final DBObjectRef<DBColumn> parentColumn;

    public NestedTableValue(@NotNull DBObjectRef<DBNestedTable> nestedTable, @NotNull DBObjectRef<DBColumn> parentColumn) {
        this.nestedTable = nestedTable;
        this.parentColumn = parentColumn;
    }

    public DBNestedTable getNestedTable() {
        return nestedTable.get();
    }

    public DBColumn getParentColumn() {
        return parentColumn.get();
    }

    @Override
    public GenericDataType getGenericDataType() {
        return TABLE;
    }

    @Override
    protected String convertUserValue(@Nullable Object userValue, DBDataType dataType, DBNConnection connection) throws SQLException {
        throw unsupported();
    }

    @Override
    public String read() {
        return DISPLAY_VALUE;
    }

    @Override
    public String read(int maxSize) {
        return DISPLAY_VALUE;
    }

    @Override
    public String export() {
        return DISPLAY_VALUE;
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, String value) throws SQLException {
        throw unsupported();
    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, String value) throws SQLException {
        throw unsupported();
    }

    @NonNls
    @Override
    public String getDisplayValue() {
        return DISPLAY_VALUE;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void release() {
    }

    private static SQLFeatureNotSupportedException unsupported() {
        return new SQLFeatureNotSupportedException("Nested table values are read-only");
    }
}
