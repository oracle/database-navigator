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
import com.dbn.connection.jdbc.Structures;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/** A SQL structured value, retaining its JDBC {@link Struct}. */
@Getter
public class StructureValue extends ValueAdapter<Struct> {
    @NonNls
    public static final String DISPLAY_VALUE = "[STRUCT]";

    private Struct struct;
    private String displayValue;

    public StructureValue() {
    }

    public StructureValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        this(readStruct(callableStatement.getObject(parameterIndex)), callableStatement.getString(parameterIndex));
    }

    public StructureValue(ResultSet resultSet, int columnIndex) throws SQLException {
        this(readStruct(resultSet.getObject(columnIndex)), resultSet.getString(columnIndex));
    }

    public StructureValue(@Nullable Struct struct, @Nullable String displayValue) {
        this.struct = struct;
        this.displayValue = displayValue == null ? formatDisplayValue(struct) : displayValue;
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.STRUCTURE;
    }

    @Override
    @Nullable
    protected Struct convertUserValue(@Nullable Object userValue, DBDataType dataType, DBNConnection connection) throws SQLException {
        if (userValue == null) {
            return null;
        } else if (userValue instanceof Struct value) {
            return value;
        } else if (userValue instanceof Object[] attributeValues) {
            var declaredType = dataType.getDeclaredType();
            if (declaredType == null) {
                throw new SQLException("Cannot create a structure without a declared type");
            }
            return Structures.createStruct(connection, declaredType, attributeValues);
        } else {
            throw new SQLException("Expected a Struct or structure attribute array, got " + userValue.getClass().getName());
        }
    }

    @Nullable
    @Override
    public Struct read() {
        return struct;
    }

    @Nullable
    public Struct getStruct() {
        return struct;
    }

    @Nullable
    @Override
    public String export() {
        return displayValue;
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable Struct value) throws SQLException {
        preparedStatement.setObject(parameterIndex, value);
        setValue(value);
    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable Struct value) throws SQLException {
        resultSet.updateObject(columnIndex, value);
        setValue(value);
    }

    @NonNls
    @Override
    public String getDisplayValue() {
        return DISPLAY_VALUE;
    }

    @Override
    public boolean matches(@Nullable Object value) throws SQLException {
        Struct other;
        if (value instanceof StructureValue structureValue) {
            other = structureValue.struct;
        } else if (value instanceof Struct struct) {
            other = struct;
        } else if (value == null) {
            other = null;
        } else {
            return false;
        }
        if (struct == other) return true;
        if (struct == null || other == null) return false;
        if (!Objects.equals(struct.getSQLTypeName(), other.getSQLTypeName())) return false;

        Object[] attributes = struct.getAttributes();
        Object[] otherAttributes = other.getAttributes();
        if (attributes.length != otherAttributes.length) return false;
        for (int i = 0; i < attributes.length; i++) {
            if (!matches(attributes[i], otherAttributes[i])) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }

    private void setValue(@Nullable Struct value) {
        struct = value;
        displayValue = formatDisplayValue(value);
    }

    @Nullable
    private static String formatDisplayValue(@Nullable Struct value) {
        if (value == null) return null;
        try {
            String typeName = Objects.toString(value.getSQLTypeName(), "STRUCT");
            StringJoiner attributes = new StringJoiner(", ", typeName + "(", ")");
            for (Object attribute : value.getAttributes()) {
                attributes.add(formatAttribute(attribute));
            }
            return attributes.toString();
        } catch (SQLException e) {
            conditionallyLog(e);
            return value.toString();
        }
    }

    private static String formatAttribute(Object value) throws SQLException {
        if (value instanceof Struct struct) return formatDisplayValue(struct);
        if (value instanceof Object[] values) return Arrays.deepToString(values);
        return Objects.toString(value, "NULL");
    }

    @Nullable
    private static Struct readStruct(Object value) throws SQLException {
        if (value == null) return null;
        if (value instanceof Struct struct) return struct;
        throw new SQLException("Expected a JDBC Struct for a declared type value, got " + value.getClass().getName());
    }

    private static boolean matches(Object value, Object other) throws SQLException {
        if (value == other) return true;
        if (value == null || other == null) return false;
        if (value instanceof Struct struct && other instanceof Struct otherStruct) {
            return new StructureValue(struct, null).matches(otherStruct);
        }
        if (value instanceof Object[] values && other instanceof Object[] otherValues) {
            if (values.length != otherValues.length) return false;
            for (int i = 0; i < values.length; i++) {
                if (!matches(values[i], otherValues[i])) return false;
            }
            return true;
        }
        return Objects.deepEquals(value, other);
    }
}
