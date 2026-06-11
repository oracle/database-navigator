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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.dbn.common.data.Data.asCharacter;

@UtilityClass
public class ResultSets {
    public static Object getColumnValue(ResultSet resultSet, int columnIndex, Class<?> type) throws SQLException {
        if (type == byte.class)        return resultSet.getByte(columnIndex);
        if (type == short.class)       return resultSet.getShort(columnIndex);
        if (type == int.class)         return resultSet.getInt(columnIndex);
        if (type == long.class)        return resultSet.getLong(columnIndex);
        if (type == float.class)       return resultSet.getFloat(columnIndex);
        if (type == double.class)      return resultSet.getDouble(columnIndex);
        if (type == boolean.class)     return resultSet.getBoolean(columnIndex);
        if (type == char.class)        return asCharacter(resultSet.getString(columnIndex));
        if (type == String.class)      return resultSet.getString(columnIndex);
        if (type == Byte.class)        return resultSet.getByte(columnIndex);
        if (type == Short.class)       return resultSet.getShort(columnIndex);
        if (type == Integer.class)     return resultSet.getInt(columnIndex);
        if (type == Long.class)        return resultSet.getLong(columnIndex);
        if (type == Float.class)       return resultSet.getFloat(columnIndex);
        if (type == Double.class)      return resultSet.getDouble(columnIndex);
        if (type == Boolean.class)     return resultSet.getBoolean(columnIndex);
        if (type == Character.class)   return asCharacter(resultSet.getString(columnIndex));
        if (type == BigDecimal.class)  return resultSet.getBigDecimal(columnIndex);
        if (type == BigInteger.class)  return getBigInteger(resultSet, columnIndex);
        if (type == Number.class)      return resultSet.getBigDecimal(columnIndex);
        if (type == Date.class)        return resultSet.getDate(columnIndex);
        if (type == Time.class)        return resultSet.getTime(columnIndex);
        if (type == Timestamp.class)   return resultSet.getTimestamp(columnIndex);

        return resultSet.getObject(columnIndex);
    }

    public static void updateColumnValue(ResultSet resultSet, int columnIndex, Object value) throws SQLException {
        Class<?> type = value == null ? Object.class : value.getClass();
        updateColumnValue(resultSet, columnIndex, value, type);
    }

    public static void updateColumnValue(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (updateNull(resultSet, columnIndex, value, type)) return;
        if (updatePrimitive(resultSet, columnIndex, value, type)) return;
        if (updateScalar(resultSet, columnIndex, value, type)) return;
        if (updateNumber(resultSet, columnIndex, value, type)) return;
        if (updateDateTime(resultSet, columnIndex, value, type)) return;
        if (updateAtomic(resultSet, columnIndex, value, type)) return;

        resultSet.updateObject(columnIndex, value);
    }

    private static boolean updateNull(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (value != null || type.isPrimitive()) return false;

        resultSet.updateObject(columnIndex, null);
        return true;
    }

    private static boolean updatePrimitive(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (type == boolean.class) resultSet.updateBoolean(columnIndex, Data.asBooleanPrimitive(value)); else
        if (type == byte.class)    resultSet.updateByte(columnIndex, Data.asBytePrimitive(value)); else
        if (type == char.class)    resultSet.updateString(columnIndex, String.valueOf(Data.asCharacterPrimitive(value))); else
        if (type == double.class)  resultSet.updateDouble(columnIndex, Data.asDoublePrimitive(value)); else
        if (type == float.class)   resultSet.updateFloat(columnIndex, Data.asFloatPrimitive(value)); else
        if (type == int.class)     resultSet.updateInt(columnIndex, Data.asIntegerPrimitive(value)); else
        if (type == long.class)    resultSet.updateLong(columnIndex, Data.asLongPrimitive(value)); else
        if (type == short.class)   resultSet.updateShort(columnIndex, Data.asShortPrimitive(value)); else return false;

        return true;
    }

    private static boolean updateScalar(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (type == Boolean.class)   return updateBoolean(resultSet, columnIndex, Data.asBoolean(value));
        if (type == Byte.class)      return updateByte(resultSet, columnIndex, Data.asByte(value));
        if (type == Character.class) return updateCharacter(resultSet, columnIndex, asCharacter(value));
        if (type == Double.class)    return updateDouble(resultSet, columnIndex, Data.asDouble(value));
        if (type == Float.class)     return updateFloat(resultSet, columnIndex, Data.asFloat(value));
        if (type == Integer.class)   return updateInteger(resultSet, columnIndex, Data.asInteger(value));
        if (type == Long.class)      return updateLong(resultSet, columnIndex, Data.asLong(value));
        if (type == Short.class)     return updateShort(resultSet, columnIndex, Data.asShort(value));
        if (type == String.class)    return updateString(resultSet, columnIndex, Data.asString(value));

        return false;
    }

    private static boolean updateNumber(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (type == BigDecimal.class) return updateBigDecimal(resultSet, columnIndex, Data.asBigDecimal(value));
        if (type == BigInteger.class) return updateBigInteger(resultSet, columnIndex, Data.asBigInteger(value));
        if (type == Number.class)     return updateBigDecimal(resultSet, columnIndex, Data.asBigDecimal(value));

        return false;
    }

    private static boolean updateDateTime(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (type == Date.class)      return updateDate(resultSet, columnIndex, (Date) value);
        if (type == Time.class)      return updateTime(resultSet, columnIndex, (Time) value);
        if (type == Timestamp.class) return updateTimestamp(resultSet, columnIndex, (Timestamp) value);

        return false;
    }

    private static boolean updateAtomic(ResultSet resultSet, int columnIndex, Object value, Class<?> type) throws SQLException {
        if (type == AtomicBoolean.class) return updateBoolean(resultSet, columnIndex, atomicBoolean(value));
        if (type == AtomicInteger.class) return updateInteger(resultSet, columnIndex, atomicInteger(value));
        if (type == AtomicLong.class)    return updateLong(resultSet, columnIndex, atomicLong(value));

        return false;
    }

    private static Boolean atomicBoolean(Object value) {
        return value instanceof AtomicBoolean atomicBoolean ? atomicBoolean.get() : Data.asBooleanPrimitive(value);
    }

    private static BigInteger getBigInteger(ResultSet resultSet, int columnIndex) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(columnIndex);
        return value == null ? null : value.toBigInteger();
    }

    private static Integer atomicInteger(Object value) {
        return value instanceof AtomicInteger atomicInteger ? atomicInteger.get() : Data.asIntegerPrimitive(value);
    }

    private static Long atomicLong(Object value) {
        return value instanceof AtomicLong atomicLong ? atomicLong.get() : Data.asLongPrimitive(value);
    }

    private static boolean updateBoolean(ResultSet resultSet, int columnIndex, Boolean value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateBoolean(columnIndex, value);
        return true;
    }

    private static boolean updateByte(ResultSet resultSet, int columnIndex, Byte value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateByte(columnIndex, value);
        return true;
    }

    private static boolean updateCharacter(ResultSet resultSet, int columnIndex, Character value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateString(columnIndex, value.toString());
        return true;
    }

    private static boolean updateDouble(ResultSet resultSet, int columnIndex, Double value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateDouble(columnIndex, value);
        return true;
    }

    private static boolean updateFloat(ResultSet resultSet, int columnIndex, Float value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateFloat(columnIndex, value);
        return true;
    }

    private static boolean updateInteger(ResultSet resultSet, int columnIndex, Integer value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateInt(columnIndex, value);
        return true;
    }

    private static boolean updateLong(ResultSet resultSet, int columnIndex, Long value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateLong(columnIndex, value);
        return true;
    }

    private static boolean updateShort(ResultSet resultSet, int columnIndex, Short value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateShort(columnIndex, value);
        return true;
    }

    private static boolean updateString(ResultSet resultSet, int columnIndex, String value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateString(columnIndex, value);
        return true;
    }

    private static boolean updateBigDecimal(ResultSet resultSet, int columnIndex, BigDecimal value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateBigDecimal(columnIndex, value);
        return true;
    }

    private static boolean updateBigInteger(ResultSet resultSet, int columnIndex, BigInteger value) throws SQLException {
        return updateBigDecimal(resultSet, columnIndex, value == null ? null : new BigDecimal(value));
    }

    private static boolean updateDate(ResultSet resultSet, int columnIndex, Date value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateDate(columnIndex, value);
        return true;
    }

    private static boolean updateTime(ResultSet resultSet, int columnIndex, Time value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateTime(columnIndex, value);
        return true;
    }

    private static boolean updateTimestamp(ResultSet resultSet, int columnIndex, Timestamp value) throws SQLException {
        if (value == null) resultSet.updateObject(columnIndex, null); else resultSet.updateTimestamp(columnIndex, value);
        return true;
    }
}
