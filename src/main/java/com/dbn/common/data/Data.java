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

import com.dbn.common.util.Chars;
import com.dbn.common.util.Csvs;
import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.dbn.common.util.Strings.firstCharacter;
import static com.dbn.common.util.Unsafe.cast;
import static java.util.Collections.singletonList;

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

    @NonNls
    public static final String NULL = "null";

    public static <T> T asType(@Nullable Object object, Class<T> type) {
        if (object == null) return null;

        Class<?> objectType = object.getClass();
        if (type.isAssignableFrom(objectType)) return cast(object);

        if (type == Boolean.class)    return cast(asBoolean(object));
        if (type == Byte.class)       return cast(asByte(object));
        if (type == Character.class)  return cast(asCharacter(object));
        if (type == Double.class)     return cast(asDouble(object));
        if (type == Float.class)      return cast(asFloat(object));
        if (type == Integer.class)    return cast(asInteger(object));
        if (type == Long.class)       return cast(asLong(object));
        if (type == Short.class)      return cast(asShort(object));
        if (type == String.class)     return cast(asString(object));
        if (type == boolean.class)    return cast(asBooleanPrimitive(object));
        if (type == byte.class)       return cast(asBytePrimitive(object));
        if (type == char.class)       return cast(asCharacterPrimitive(object));
        if (type == double.class)     return cast(asDoublePrimitive(object));
        if (type == float.class)      return cast(asFloatPrimitive(object));
        if (type == int.class)        return cast(asIntegerPrimitive(object));
        if (type == long.class)       return cast(asLongPrimitive(object));
        if (type == short.class)      return cast(asShortPrimitive(object));
        if (type == BigDecimal.class) return cast(asBigDecimal(object));
        if (type == BigInteger.class) return cast(asBigInteger(object));
        if (type == Object.class)     return cast(object);

        if (type.isEnum())            return cast(asEnum(type, object));
        if (type == String[].class)   return cast(asStringArray(object));

        throw new UnsupportedOperationException("Cast from " + objectType + " to " + type + " is not implemented");
        // TODO add more cast logic if required
    }

    public static <T> List<T> asTypeList(@Nullable Object object, Class<T> type) {
        return asList(object, o -> asType(o, type));
    }

    @Nullable
    public static String asString(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof char[] chars) return Chars.toString(chars);
        return object.toString();
    }


    public static List<String> asStringList(@Nullable Object object) {
        return asList(object, o -> asString(o));
    }

    public static String[] asStringArray(@Nullable Object object) {
        List<String> strings =
                object instanceof String string ?
                        csvToList(string, String.class) : // assumed csv
                        asList(object, o -> asString(o));
        return strings == null ? new String[0] : strings.toArray(new String[0]);
    }

    public static Enum asEnum(Class<?> type, Object object) {
        if (object == null) return null;

        String string = asString(object);
        if (string == null) return null;

        return Enum.valueOf((Class<Enum>)type, string);
    }

    public static Character asCharacter(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Character) return (Character) object;
        if (object.equals(NULL)) return null;
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
        return asNumber(object, s -> Integer.valueOf(s));
    }

    public static int asIntegerPrimitive(@Nullable Object object) {
        Integer integer = asInteger(object);
        return integer == null ? 0 : integer;
    }

    public static Byte asByte(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Byte) return (Byte) object;
        if (object instanceof Number) return ((Number) object).byteValue();
        return asNumber(object, s -> Byte.valueOf(s));
    }

    public static byte asBytePrimitive(@Nullable Object object) {
        Byte byteValue = asByte(object);
        return byteValue == null ? 0 : byteValue;
    }

    public static Short asShort(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Short) return (Short) object;
        if (object instanceof Number) return ((Number) object).shortValue();
        return asNumber(object, s -> Short.valueOf(s));
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
        return asNumber(object, s -> Long.valueOf(s));
    }

    public static long asLongPrimitive(@Nullable Object object) {
        Long longVal = asLong(object);
        return longVal == null ? 0 : longVal;
    }

    public static Double asDouble(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Double) return (Double) object;
        if (object instanceof Number) return ((Number) object).doubleValue();
        return asNumber(object, s -> Double.valueOf(s));
    }

    public static List<Double> asDoubleList(@Nullable Object object) {
        return asTypeList(object, Double.class);
    }

    public static double asDoublePrimitive(@Nullable Object object) {
        Double doubleVal = asDouble(object);
        return doubleVal == null ? 0 : doubleVal;
    }

    public static Float asFloat(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Float) return (Float) object;
        if (object instanceof Number) return ((Number) object).floatValue();
        return asNumber(object, s -> Float.valueOf(s));
    }

    public static float asFloatPrimitive(@Nullable Object object) {
        Float floatVal = asFloat(object);
        return floatVal == null ? 0 : floatVal;
    }

    @Nullable
    public static Boolean asBoolean(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Boolean) return (Boolean) object;
        if (object.equals(NULL)) return null;
        if (object instanceof String) return Strings.isOneOfIgnoreCase((String) object, "Y", "YES", "T", "TRUE", "1");
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
        return asNumber(object, s -> new BigDecimal(s));
    }

    public static BigInteger asBigInteger(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof BigInteger) return (BigInteger) object;
        return asNumber(object, s -> new BigInteger(s));
    }

    public static Class<?> asPrimitiveClass(Class<?> type) {
        if (type.isPrimitive()) return type;

        if (type == Boolean.class) return boolean.class;
        if (type == Byte.class) return byte.class;
        if (type == Character.class) return char.class;
        if (type == Short.class) return short.class;
        if (type == Integer.class) return int.class;
        if (type == Long.class) return long.class;
        if (type == Float.class) return float.class;
        if (type == Double.class) return double.class;
        if (type == Void.class) return void.class;

        return null;
    }

    public static Class<?> asPrimitiveClass(@NonNls String primitiveTypeName) {
        if (primitiveTypeName == null) return null;

        return switch (primitiveTypeName) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> null;
        };
    }


    private static <T> List<T> asList(@Nullable Object object, Function<Object, T> converter) {
        if (object == null) return null;
        if (object instanceof Iterable) {
            List<T> list = new ArrayList<>();
            Iterable<?> iterable = (Iterable) object;
            iterable.forEach(o -> list.add(converter.apply(o)));
            return list;
        }
        if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            List<T> list = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                Object element = Array.get(object, i);
                list.add(converter.apply(element));
            }
            return list;
        }
        return singletonList(converter.apply(object));
    }

    public static <S, T> T[] convert(S[] array, Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, array.length);
        for (int i = 0; i < array.length; i++) {
            Object o = array[i];
            result[i] = asType(o, type);
        }
        return result;
    }

    public static <T> T[] convert(double[] array, Class<T> type) {
        T[] result = (T[]) Array.newInstance(type, array.length);
        for (int i = 0; i < array.length; i++) {
            double d = array[i];
            result[i] = asType(d, type);
        }
        return result;
    }

    @Nullable
    private static <T extends Number> T asNumber(Object object, Function<String, T> converter) {
        if (object == null) return null;
        String string = object.toString().trim();
        if (string.isEmpty()) return null;
        if (string.equals(NULL)) return null;

        return converter.apply(string);
    }

    public static <T> String listToCsv(List<T> list) {
        return Csvs.valuesToCsv(list, v -> asString(v));
    }

    public static <T> List<T> csvToList(String csv, Class<T> type) {
        return Csvs.csvToValues(csv, v -> asType(v, type));
    }

    public static <T> String listToArrayString(List<T> list) {
        if (list == null) return null;
        if (list.isEmpty()) return "[]";
        return "[" + listToCsv(list) + "]";
    }

    public static <T> List<T> arrayStringToList(String arrayString, Class<T> type) {
        if (arrayString == null) return null;
        if (arrayString.isEmpty()) return null;
        if (arrayString.startsWith("[") && arrayString.endsWith("]")) {
            arrayString = arrayString.substring(1, arrayString.length() - 1);
        }
        return csvToList(arrayString, type);

    }
}
