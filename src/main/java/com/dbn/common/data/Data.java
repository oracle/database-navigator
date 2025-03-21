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

package com.dbn.common.data;

import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.dbn.common.util.Strings.firstCharacter;

/**
 * Utility class for type conversion and type casting operations. It provides a
 * series of static methods to convert an object to a specified type, including
 * primitive types, wrapper classes, and some common data types such as
 * BigDecimal and BigInteger. This class contains a general cast method to
 * facilitate safe type casting and a variety of methods for converting objects
 * to specific types.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public final class Data {

    public static <T> T cast(@Nullable Object object, Class<T> type) {
        if (object == null) return null;

        if (type == Boolean.class)    return Unsafe.cast(asBoolean(object));
        if (type == Character.class)  return Unsafe.cast(asCharacter(object));
        if (type == Double.class)     return Unsafe.cast(asDouble(object));
        if (type == Float.class)      return Unsafe.cast(asFloat(object));
        if (type == Integer.class)    return Unsafe.cast(asInteger(object));
        if (type == Long.class)       return Unsafe.cast(asLong(object));
        if (type == Short.class)      return Unsafe.cast(asShort(object));
        if (type == String.class)     return Unsafe.cast(asString(object));
        if (type == boolean.class)    return Unsafe.cast(asBooleanPrimitive(object));
        if (type == char.class)       return Unsafe.cast(asCharacterPrimitive(object));
        if (type == double.class)     return Unsafe.cast(asDoublePrimitive(object));
        if (type == float.class)      return Unsafe.cast(asFloatPrimitive(object));
        if (type == int.class)        return Unsafe.cast(asIntegerPrimitive(object));
        if (type == long.class)       return Unsafe.cast(asLongPrimitive(object));
        if (type == short.class)      return Unsafe.cast(asShortPrimitive(object));
        if (type == BigDecimal.class) return Unsafe.cast(asBigDecimal(object));
        if (type == BigInteger.class) return Unsafe.cast(asBigInteger(object));

        throw new UnsupportedOperationException("Cast from " + object.getClass() + " to " + type + " is not implemented");
        // TODO add more cast logic if required
    }

    @Nullable
    public static String asString(@Nullable Object object) {
        if (object == null) return null;
        return object.toString();
    }

    public static Character asCharacter(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Character) return (Character) object;
        return firstCharacter(object.toString());
    }

    public static char asCharacterPrimitive(@Nullable Object object) {
        Character character = asCharacter(object);
        return character == null ? 0 : character;
    }

    @Nullable
    public static Integer asInteger(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Integer) return (Integer) object;
        if (object instanceof Number) return ((Number) object).intValue();
        return Integer.valueOf(object.toString());
    }

    public static int asIntegerPrimitive(@Nullable Object object) {
        Integer integer = asInteger(object);
        return integer == null ? 0 : integer;
    }

    public static Byte asByte(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Byte) return (Byte) object;
        if (object instanceof Number) return ((Number) object).byteValue();
        return Byte.valueOf(object.toString());
    }

    public static byte asBytePrimitive(@Nullable Object object) {
        Byte byteValue = asByte(object);
        return byteValue == null ? 0 : byteValue;
    }

    public static Short asShort(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Short) return (Short) object;
        if (object instanceof Number) return ((Number) object).shortValue();
        return Short.valueOf(object.toString());
    }

    public static short asShortPrimitive(@Nullable Object object) {
        Short shrt = asShort(object);
        return shrt == null ? 0 : shrt;
    }

    @Nullable
    public static Long asLong(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Long) return (Long) object;
        if (object instanceof Number) return ((Number) object).longValue();
        return Long.valueOf(object.toString());
    }

    public static long asLongPrimitive(@Nullable Object object) {
        Long longVal = asLong(object);
        return longVal == null ? 0 : longVal;
    }

    public static Double asDouble(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Double) return (Double) object;
        if (object instanceof Number) return ((Number) object).doubleValue();
        return Double.valueOf(object.toString());
    }

    public static double asDoublePrimitive(@Nullable Object object) {
        Double doubleVal = asDouble(object);
        return doubleVal == null ? 0 : doubleVal;
    }

    public static Float asFloat(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Float) return (Float) object;
        if (object instanceof Number) return ((Number) object).floatValue();
        return Float.valueOf(object.toString());
    }

    public static float asFloatPrimitive(@Nullable Object object) {
        Float floatVal = asFloat(object);
        return floatVal == null ? 0 : floatVal;
    }

    @Nullable
    public static Boolean asBoolean(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Boolean) return (Boolean) object;
        if (object instanceof String) return Strings.isOneOfIgnoreCase((String) object, "Y", "YES", "TRUE", "1");
        if (object instanceof Number) return ((Number) object).intValue() != 0;
        return null;
    }

    public static boolean asBooleanPrimitive(@Nullable Object object) {
        Boolean bool = asBoolean(object);
        return bool != null && bool;
    }

    public static BigDecimal asBigDecimal(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof BigDecimal) return (BigDecimal) object;
        return new BigDecimal(object.toString());
    }

    public static BigInteger asBigInteger(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof BigInteger) return (BigInteger) object;
        return new BigInteger(object.toString());
    }

    public static Class<?> primitive(Class<?> type) {
        if (type.isPrimitive()) return type;

        if (type == Byte.class) return byte.class;
        if (type == Character.class) return char.class;
        if (type == Short.class) return short.class;
        if (type == Integer.class) return int.class;
        if (type == Long.class) return long.class;
        if (type == Float.class) return float.class;
        if (type == Double.class) return double.class;

        return null;
    }

    public static <S, T> T[] convert(S[] array, Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, array.length);
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            result[i] = cast(o, type);
        }
        return result;
    }

    public static <T> T[] convert(double[] array, Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, array.length);
        for (int i = 0; i < array.length; i++) {
            double d = array[i];
            result[i] = cast(d, type);
        }
        return result;
    }
}
