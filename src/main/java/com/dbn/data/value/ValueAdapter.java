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

package com.dbn.data.value;

import com.dbn.data.type.GenericDataType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class ValueAdapter<T> {

    public abstract GenericDataType getGenericDataType();
    public abstract @Nullable T read() throws SQLException;
    public abstract @Nullable String export() throws SQLException;
    public abstract void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable T value) throws SQLException;
    public abstract void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable T value) throws SQLException;

    @NonNls
    public abstract String getDisplayValue();

    public static final Map<GenericDataType, Class<? extends ValueAdapter<?>>> REGISTRY = new EnumMap<>(GenericDataType.class);
    static {
        REGISTRY.put(GenericDataType.ARRAY, ArrayValue.class);
        REGISTRY.put(GenericDataType.BLOB, BlobValue.class);
        REGISTRY.put(GenericDataType.CLOB, ClobValue.class);
        REGISTRY.put(GenericDataType.NCLOB, NClobValue.class);
        REGISTRY.put(GenericDataType.XMLTYPE, XmlTypeValue.class);
    }

    public static boolean supports(GenericDataType genericDataType) {
        return REGISTRY.containsKey(genericDataType);
    }

    private static <T> Class<ValueAdapter<T>> get(GenericDataType genericDataType) {
        return cast(REGISTRY.get(genericDataType));
    }

    public static <T> ValueAdapter<T> create(GenericDataType genericDataType) throws SQLException {
        try {
            Class<ValueAdapter<T>> valueAdapterClass = get(genericDataType);
            return valueAdapterClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            conditionallyLog(e);
            handleException(e, genericDataType);
        }
        return null;
    }

    public static <T> ValueAdapter<T> create(GenericDataType genericDataType, ResultSet resultSet, int columnIndex) throws SQLException {
        try {
            Class<ValueAdapter<T>> valueAdapterClass = get(genericDataType);
            Constructor<ValueAdapter<T>> constructor = valueAdapterClass.getConstructor(ResultSet.class, int.class);
            return constructor.newInstance(resultSet, columnIndex);
        } catch (Throwable e) {
            conditionallyLog(e);
            handleException(e, genericDataType);
        }
        return null;
    }

    public static <T> ValueAdapter<T> create(GenericDataType genericDataType, CallableStatement callableStatement, int parameterIndex) throws SQLException {
        Class<ValueAdapter<T>> valueAdapterClass = get(genericDataType);
        try {
            Constructor<ValueAdapter<T>> constructor = valueAdapterClass.getConstructor(CallableStatement.class, int.class);
            return constructor.newInstance(callableStatement, parameterIndex);
        } catch (Throwable e) {
            conditionallyLog(e);
            handleException(e, genericDataType);
            return null;
        }
    }

    private static void handleException(Throwable e, GenericDataType genericDataType) throws SQLException {
        if (e instanceof InvocationTargetException) {
            InvocationTargetException invocationTargetException = (InvocationTargetException) e;
            e = invocationTargetException.getTargetException();
        }
        if (e instanceof SQLException) {
            throw (SQLException) e;
        } else {
            log.error("Error creating value adapter for generic type " + genericDataType.name() + '.', e);
            throw new SQLException("Error creating value adapter for generic type " + genericDataType.name() + '.', e);
        }
    }
}
