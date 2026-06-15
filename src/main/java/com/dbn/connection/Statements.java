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

package com.dbn.connection;

import com.dbn.common.data.Data;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class Statements {
    public static void setParameterValue(PreparedStatement statement, int parameterIndex, Object value) throws SQLException {
        Class<?> type = value == null ? Object.class : value.getClass();
        setParameterValue(statement, parameterIndex, value, type);
    }

    public static void setParameterValue(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (setNull(statement, parameterIndex, value, type)) return;
        if (setPrimitive(statement, parameterIndex, value, type)) return;
        if (setScalar(statement, parameterIndex, value, type)) return;
        if (setNumber(statement, parameterIndex, value, type)) return;
        if (setDateTime(statement, parameterIndex, value, type)) return;
        if (setAtomic(statement, parameterIndex, value, type)) return;

        statement.setObject(parameterIndex, value);
    }

    private static boolean setNull(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (value != null || type.isPrimitive()) return false;

        statement.setObject(parameterIndex, null);
        return true;
    }

    private static boolean setPrimitive(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (type == boolean.class) statement.setBoolean(parameterIndex, Data.asBooleanPrimitive(value)); else
        if (type == byte.class)    statement.setByte(parameterIndex, Data.asBytePrimitive(value)); else
        if (type == char.class)    statement.setString(parameterIndex, String.valueOf(Data.asCharacterPrimitive(value))); else
        if (type == double.class)  statement.setDouble(parameterIndex, Data.asDoublePrimitive(value)); else
        if (type == float.class)   statement.setFloat(parameterIndex, Data.asFloatPrimitive(value)); else
        if (type == int.class)     statement.setInt(parameterIndex, Data.asIntegerPrimitive(value)); else
        if (type == long.class)    statement.setLong(parameterIndex, Data.asLongPrimitive(value)); else
        if (type == short.class)   statement.setShort(parameterIndex, Data.asShortPrimitive(value)); else return false;

        return true;
    }

    private static boolean setScalar(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (type == Boolean.class)   return setBoolean(statement, parameterIndex, Data.asBoolean(value));
        if (type == Byte.class)      return setByte(statement, parameterIndex, Data.asByte(value));
        if (type == Character.class) return setCharacter(statement, parameterIndex, Data.asCharacter(value));
        if (type == Double.class)    return setDouble(statement, parameterIndex, Data.asDouble(value));
        if (type == Float.class)     return setFloat(statement, parameterIndex, Data.asFloat(value));
        if (type == Integer.class)   return setInteger(statement, parameterIndex, Data.asInteger(value));
        if (type == Long.class)      return setLong(statement, parameterIndex, Data.asLong(value));
        if (type == Short.class)     return setShort(statement, parameterIndex, Data.asShort(value));
        if (type == String.class)    return setString(statement, parameterIndex, Data.asString(value));

        return false;
    }

    private static boolean setNumber(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (type == BigDecimal.class) return setBigDecimal(statement, parameterIndex, Data.asBigDecimal(value));
        if (type == BigInteger.class) return setBigInteger(statement, parameterIndex, Data.asBigInteger(value));
        if (type == Number.class)     return setBigDecimal(statement, parameterIndex, Data.asBigDecimal(value));

        return false;
    }

    private static boolean setDateTime(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (type == Date.class)      return setDate(statement, parameterIndex, (Date) value);
        if (type == Time.class)      return setTime(statement, parameterIndex, (Time) value);
        if (type == Timestamp.class) return setTimestamp(statement, parameterIndex, (Timestamp) value);

        return false;
    }

    private static boolean setAtomic(PreparedStatement statement, int parameterIndex, Object value, Class<?> type) throws SQLException {
        if (type == AtomicBoolean.class) return setBoolean(statement, parameterIndex, atomicBoolean(value));
        if (type == AtomicInteger.class) return setInteger(statement, parameterIndex, atomicInteger(value));
        if (type == AtomicLong.class)    return setLong(statement, parameterIndex, atomicLong(value));

        return false;
    }

    private static Boolean atomicBoolean(Object value) {
        return value instanceof AtomicBoolean atomicBoolean ? atomicBoolean.get() : Data.asBooleanPrimitive(value);
    }

    private static Integer atomicInteger(Object value) {
        return value instanceof AtomicInteger atomicInteger ? atomicInteger.get() : Data.asIntegerPrimitive(value);
    }

    private static Long atomicLong(Object value) {
        return value instanceof AtomicLong atomicLong ? atomicLong.get() : Data.asLongPrimitive(value);
    }

    private static boolean setBoolean(PreparedStatement statement, int parameterIndex, Boolean value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setBoolean(parameterIndex, value);
        return true;
    }

    private static boolean setByte(PreparedStatement statement, int parameterIndex, Byte value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setByte(parameterIndex, value);
        return true;
    }

    private static boolean setCharacter(PreparedStatement statement, int parameterIndex, Character value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setString(parameterIndex, value.toString());
        return true;
    }

    private static boolean setDouble(PreparedStatement statement, int parameterIndex, Double value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setDouble(parameterIndex, value);
        return true;
    }

    private static boolean setFloat(PreparedStatement statement, int parameterIndex, Float value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setFloat(parameterIndex, value);
        return true;
    }

    private static boolean setInteger(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setInt(parameterIndex, value);
        return true;
    }

    private static boolean setLong(PreparedStatement statement, int parameterIndex, Long value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setLong(parameterIndex, value);
        return true;
    }

    private static boolean setShort(PreparedStatement statement, int parameterIndex, Short value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setShort(parameterIndex, value);
        return true;
    }

    private static boolean setString(PreparedStatement statement, int parameterIndex, String value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setString(parameterIndex, value);
        return true;
    }

    private static boolean setBigDecimal(PreparedStatement statement, int parameterIndex, BigDecimal value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setBigDecimal(parameterIndex, value);
        return true;
    }

    private static boolean setBigInteger(PreparedStatement statement, int parameterIndex, BigInteger value) throws SQLException {
        return setBigDecimal(statement, parameterIndex, value == null ? null : new BigDecimal(value));
    }

    private static boolean setDate(PreparedStatement statement, int parameterIndex, Date value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setDate(parameterIndex, value);
        return true;
    }

    private static boolean setTime(PreparedStatement statement, int parameterIndex, Time value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setTime(parameterIndex, value);
        return true;
    }

    private static boolean setTimestamp(PreparedStatement statement, int parameterIndex, Timestamp value) throws SQLException {
        if (value == null) statement.setObject(parameterIndex, null); else statement.setTimestamp(parameterIndex, value);
        return true;
    }
}
