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


@UtilityClass
public final class Data {

    public static <T> T cast(@Nullable Object object, Class<T> type) {
        if (object != null) {
            if (type == String.class)   return Unsafe.cast(asString(object));
            if (type == Short.class)    return Unsafe.cast(asShort(object));
            if (type == Integer.class)  return Unsafe.cast(asInteger(object));
            if (type == Long.class)     return Unsafe.cast(asLong(object));
            if (type == Boolean.class)  return Unsafe.cast(asBoolean(object));
            if (type == short.class)    return Unsafe.cast(asShrt(object));
            if (type == int.class)      return Unsafe.cast(asInt(object));
            if (type == long.class)     return Unsafe.cast(asLng(object));
            if (type == boolean.class)  return Unsafe.cast(asBool(object));

            throw new UnsupportedOperationException("Cast from " + object.getClass() + " to " + type + " is not implemented");
            // TODO add more cast logic if required
        }
        return null;
    }

    @Nullable
    public static String asString(@Nullable Object object) {
        if (object == null) return null;
        return object.toString();
    }

    @Nullable
    public static Integer asInteger(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Integer) return (Integer) object;
        if (object instanceof Number) return ((Number) object).intValue();
        return Integer.valueOf(object.toString());
    }

    public static int asInt(@Nullable Object object) {
        Integer integer = asInteger(object);
        return integer == null ? 0 : integer;
    }

    public static Short asShort(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Short) return (Short) object;
        if (object instanceof Number) return ((Number) object).shortValue();
        return Short.valueOf(object.toString());
    }

    public static short asShrt(@Nullable Object object) {
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

    public static long asLng(@Nullable Object object) {
        Long longVal = asLong(object);
        return longVal == null ? 0 : longVal;
    }

    @Nullable
    public static Boolean asBoolean(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Boolean) return (Boolean) object;
        if (object instanceof String) return Strings.isOneOfIgnoreCase((String) object, "Y", "YES", "TRUE", "1");
        if (object instanceof Number) return ((Number) object).intValue() != 0;
        return null;
    }

    public static boolean asBool(@Nullable Object object) {
        Boolean bool = asBoolean(object);
        return bool != null && bool;
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
}
