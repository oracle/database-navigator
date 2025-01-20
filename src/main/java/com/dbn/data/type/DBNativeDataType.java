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

package com.dbn.data.type;

import com.dbn.common.content.DynamicContentElement;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.data.value.ValueAdapter;
import com.dbn.database.common.util.DataTypeParseAdapter;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import static com.dbn.common.util.Unsafe.silent;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@NonNls
@Getter
public class DBNativeDataType extends StatefulDisposableBase implements DynamicContentElement{
    private final DataTypeDefinition definition;

    public DBNativeDataType(DataTypeDefinition definition) {
        this.definition = definition;
    }

    @NotNull
    @Override
    public String getName() {
        return definition.getName();
    }

    public GenericDataType getGenericDataType() {
        return definition.getGenericDataType();
    }

    public boolean isPseudoNative() {
        return definition.isPseudoNative();
    }

    public boolean isLargeObject() {
        return getGenericDataType().isLOB();
    }

    public Object getValueFromResultSet(ResultSet resultSet, int columnIndex) {
        // FIXME: add support for stream updatable types

        GenericDataType genericDataType = definition.getGenericDataType();
        if (genericDataType == GenericDataType.ROWID) return "[ROWID]";
        if (genericDataType == GenericDataType.FILE) return "[FILE]";
        if (ValueAdapter.supports(genericDataType)) return createValueAdapter(resultSet, columnIndex, genericDataType);

        Class<?> clazz = definition.getTypeClass();
        try {
            if (Number.class.isAssignableFrom(clazz) && resultSet.getString(columnIndex) == null) {
                // mysql converts null numbers to 0!!!
                // FIXME make this database dependent (e.g. in CompatibilityInterface).
                return null;
            }

            DataTypeParseAdapter parseAdapter = definition.getParseAdapter();
            if (parseAdapter != null) {
                String stringValue = resultSet.getString(columnIndex);
                return parseAdapter.parse(stringValue);
            }

            return
                    clazz == String.class ? resultSet.getString(columnIndex) :
                    clazz == Byte.class ? resultSet.getByte(columnIndex) :
                    clazz == Short.class ? resultSet.getShort(columnIndex) :
                    clazz == Integer.class ? resultSet.getInt(columnIndex) :
                    clazz == Long.class ? resultSet.getLong(columnIndex) :
                    clazz == Float.class ? resultSet.getFloat(columnIndex) :
                    clazz == Double.class ? resultSet.getDouble(columnIndex) :
                    clazz == BigDecimal.class ? resultSet.getBigDecimal(columnIndex) :
                    clazz == Date.class ? resultSet.getDate(columnIndex) :
                    clazz == Time.class ? resultSet.getTime(columnIndex) :
                    clazz == Timestamp.class ? resultSet.getTimestamp(columnIndex) :
                    clazz == Boolean.class ? resultSet.getBoolean(columnIndex) :
                    //clazz == Array.class ? resultSet.getArray(columnIndex) :
                            resultSet.getObject(columnIndex);
        } catch (Throwable e) {
            conditionallyLog(e);
            return silent(null, () -> resolveConversionFailure(resultSet, columnIndex, clazz, e));
        }
    }

    @Nullable
    private Object createValueAdapter(ResultSet resultSet, int columnIndex, GenericDataType genericDataType) {
        try {
            return ValueAdapter.create(genericDataType, resultSet, columnIndex);
        } catch (Throwable e) {
            conditionallyLog(e);
            return silent(null, () -> {
                Object object = resultSet.getObject(columnIndex);
                log.error("Error creating result-set value for {} '{}'. (data type definition {})", genericDataType, object, definition, e);
                return null;
            });
        }
    }

    @Nullable
    @SneakyThrows
    private Object resolveConversionFailure(ResultSet resultSet, int columnIndex, Class<?> expectedClass, Throwable err) {
        Object object = resultSet.getObject(columnIndex);
        // TODO odd values resolvers
        if (object instanceof String && Strings.isEmpty((String) object)) {
            return null;
        } else if (object instanceof Double && ((Double) object).isNaN()) {
            // DBNE-4151
            return null;
        } else if (object instanceof Long && Integer.class.isAssignableFrom(expectedClass)) {
            // odd jdbc implementations allowing long for data type int java.sql.Types.INTEGER
            return object;
        } else if (object instanceof Timestamp && Long.class.isAssignableFrom(expectedClass)) {
            // DBNE-432
            return ((Timestamp) object).getTime();
        } else if (object instanceof Number && java.util.Date.class.isAssignableFrom(expectedClass)) {
            // fallback for dates stored as milliseconds (sqlite?)
            Number number = (Number) object;
            long longValue = number.longValue();
            return expectedClass == Date.class ? new Date(longValue) :
                    expectedClass == Time.class ? new Time(longValue) :
                    expectedClass == Timestamp.class ? new Timestamp(longValue) : null;
        } else {
            String objectClass = object == null ? "" : object.getClass().getName();
            log.error("Error resolving result-set value for {} '{}'. (data type definition {})", objectClass, object, definition, err);
            return object;
        }
    }

    public void setValueToResultSet(ResultSet resultSet, int columnIndex, Object value) throws SQLException {
        // FIXME: add support for stream updatable types
        GenericDataType genericDataType = definition.getGenericDataType();
        if (genericDataType == GenericDataType.BLOB) return;
        if (genericDataType == GenericDataType.CLOB) return;
        if (genericDataType == GenericDataType.XMLTYPE) return;
        if (genericDataType == GenericDataType.ROWID) return;
        if (genericDataType == GenericDataType.FILE) return;
        if (genericDataType == GenericDataType.ARRAY) return;

        if (value == null) {
            resultSet.updateObject(columnIndex, null);
        } else {
            Class clazz = definition.getTypeClass();
            if (value.getClass().isAssignableFrom(clazz)) {
                if(clazz == String.class) resultSet.updateString(columnIndex, (String) value); else
                if(clazz == Byte.class) resultSet.updateByte(columnIndex, (Byte) value); else
                if(clazz == Short.class) resultSet.updateShort(columnIndex, (Short) value); else
                if(clazz == Integer.class) resultSet.updateInt(columnIndex, (Integer) value); else
                if(clazz == Long.class) resultSet.updateLong(columnIndex, (Long) value); else
                if(clazz == Float.class) resultSet.updateFloat(columnIndex, (Float) value); else
                if(clazz == Double.class) resultSet.updateDouble(columnIndex, (Double) value); else
                if(clazz == BigDecimal.class) resultSet.updateBigDecimal(columnIndex, (BigDecimal) value); else
                if(clazz == Date.class) resultSet.updateDate(columnIndex, (Date) value); else
                if(clazz == Time.class) resultSet.updateTime(columnIndex, (Time) value); else
                if(clazz == Timestamp.class) resultSet.updateTimestamp(columnIndex, (Timestamp) value); else
                if(clazz == Boolean.class) resultSet.updateBoolean(columnIndex, (Boolean) value); else
                //if(clazz == Array.class) resultSet.updateArray(columnIndex, (Array) value); else
                        resultSet.updateObject(columnIndex, value);
            } else {
                throw new SQLException("Can not convert \"" + value + "\" into " + definition.getName());
            }
        }
    }

    public Object getValueFromStatement(DBNCallableStatement callableStatement, int parameterIndex) throws SQLException {
        GenericDataType genericDataType = definition.getGenericDataType();
        if (ValueAdapter.supports(genericDataType)) {
            return ValueAdapter.create(genericDataType, callableStatement, parameterIndex);
        }
        return callableStatement.getObject(parameterIndex);
    }

    public <T> void setValueToStatement(PreparedStatement statement, int parameterIndex, T value) throws SQLException {
        GenericDataType genericDataType = definition.getGenericDataType();
        if (ValueAdapter.supports(genericDataType)) {
            ValueAdapter<T> valueAdapter = ValueAdapter.create(genericDataType);
            if (valueAdapter != null) {
                Connection connection = statement.getConnection();
                valueAdapter.write(connection, statement, parameterIndex, value);
            }
            return;
        }
        if (genericDataType == GenericDataType.CURSOR) return;// not supported

        DataTypeParseAdapter<T> parseAdapter = definition.getParseAdapter();
        if (parseAdapter != null) {
            String stringValue =  parseAdapter.toString(value);
            statement.setString(parameterIndex, stringValue);
            return;
        }

        if (value == null) {
            statement.setObject(parameterIndex, null);
        } else {
            Class clazz = definition.getTypeClass();
            if (value.getClass().isAssignableFrom(clazz)) {
                if(clazz == String.class) statement.setString(parameterIndex, (String) value); else
                if(clazz == Byte.class) statement.setByte(parameterIndex, (Byte) value); else
                if(clazz == Short.class) statement.setShort(parameterIndex, (Short) value); else
                if(clazz == Integer.class) statement.setInt(parameterIndex, (Integer) value); else
                if(clazz == Long.class) statement.setLong(parameterIndex, (Long) value); else
                if(clazz == Float.class) statement.setFloat(parameterIndex, (Float) value); else
                if(clazz == Double.class) statement.setDouble(parameterIndex, (Double) value); else
                if(clazz == BigDecimal.class) statement.setBigDecimal(parameterIndex, (BigDecimal) value); else
                if(clazz == Date.class) statement.setDate(parameterIndex, (Date) value); else
                if(clazz == Time.class) statement.setTime(parameterIndex, (Time) value); else
                if(clazz == Timestamp.class) statement.setTimestamp(parameterIndex, (Timestamp) value); else
                if(clazz == Boolean.class) statement.setBoolean(parameterIndex, (Boolean) value); else
                        statement.setObject(parameterIndex, value);
            } else {
                throw new SQLException("Can not convert \"" + value + "\" into " + definition.getName());
            }
        }
    }

    public int getSqlType(){
        return definition.getSqlType();
    }


    public String toString() {
        return definition.getName();
    }

    /*********************************************************
     *                 DynamicContentElement                 *
     *********************************************************/

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DynamicContentType getDynamicContentType() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        DBNativeDataType remote = (DBNativeDataType) o;
        return getName().compareTo(remote.getName());
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
