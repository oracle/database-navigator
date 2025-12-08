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

package com.dbn.common.util;

import com.dbn.common.routine.ParametricCallable;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.dbn.common.dispose.Failsafe.guarded;

@Slf4j
@UtilityClass
public final class Commons {

    @NotNull
    public static <T> T nvl(@Nullable T value, @NotNull Supplier<T> defaultValue) {
        return value == null ? defaultValue.get() : value;
    }

    @SafeVarargs
    @Nullable
    public static <T> T coalesce(Supplier<T>... suppliers) {
        for (Supplier<T> supplier : suppliers) {
            T value = guarded(null, supplier, s -> s.get());
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @SneakyThrows
    @SafeVarargs
    @Nullable
    public static <T, P> T coalesce(P param, ParametricCallable<P, T, Throwable>... suppliers) {
        for (ParametricCallable<P, T, Throwable> supplier : suppliers) {
            T value = guarded(null, supplier, s -> s.call(param));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @NotNull
    public static <T> T nvl(@Nullable T value, @NotNull T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static <T> T nvln(@Nullable T value, @Nullable T defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Nullable
    public static <T> T nvln(@Nullable T value, @NotNull Supplier<T> defaultValue) {
        return value == null ? defaultValue.get() : value;
    }

    public static String nullIfEmpty(String string) {
        if (string != null) {
            string = string.trim();
            if (string.length() == 0) {
                string = null;
            }
        }
        return string;
    }

    public static String readInputStream(InputStream inputStream) throws IOException {
        try (Reader in = new InputStreamReader(inputStream)) {
            StringBuilder buffer = new StringBuilder();
            int i;
            while ((i = in.read()) != -1) buffer.append((char) i);
            in.close();
            return buffer.toString();
        }
    }

    @SafeVarargs
    public static <T> boolean isOneOf(T object, T... objects) {
        for (T obj : objects) {
            if (obj == null && object == null) return true;
            if (obj == object) return true;
        }
        return false;
    }

    /**
     * Checks whether the provided array of objects is null or empty.
     *
     * @param objects an array of objects to check, can be null
     * @return true if the array is null or has no elements, otherwise false
     */
    @SafeVarargs
    public static <T> boolean isEmpty(T... objects) {
        return objects == null || objects.length == 0;
    }

    public static <T> int indexOf(T[] objects, T object) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == object) return i;
        }
        return -1;
    }

    @NotNull
    public static <T> T[] array(T... values) {
        return values;
    }

    /**
     * If object is null, return null
     * If object is an array:
     *   - if object instancof Object, just cast it back as Object[]
     *   - if the array type is a primitive then create a new array
     *     of Object and "box" up the contents of object into that array.
     * If object is not an array, wrap it in an Object[]
     *    - C.B. I'm still not sure that last one is consistent.
     *
     * @param object the object to "box into an array"
     * @return object, a new Object[] or null per above.
     */
    public static Object[] boxArray(@Nullable Object object) {
        if (object == null) return null;
        if (object instanceof Object[]) return (Object[]) object;
        if (object.getClass().isArray()) {
            int length = Array.getLength(object);
            Object[] newArray = new Object[length];
            for (int i = 0; i < length; i++) {
                newArray[i] = Array.get(object, i);
            }
            return newArray;
        }
        return new Object[] {object};
    }

    /**
     * Two values, value1 and value2 "match as Arrays" if:
     *  - both are null
     *  - they are "==" to each other
     *  - the are both arrays and their lengths are equal and all of their
     *    array values {@link Commons#match}
     *  - at least one is non-null, not an array but they are {@link Objects#deepEquals}
     * @param value1
     * @param value2
     * @return
     */
    public static boolean matchArrays(@Nullable Object value1, @Nullable Object value2) {
        if (value1 == null && value2 == null) return true;
        if (value1 == value2) return true;
        if (value1 != null && value2 != null) {
            boolean isArray1 = value1.getClass().isArray();
            boolean isArray2 = value2.getClass().isArray();
            if (isArray1 && isArray2) {
                int length1 = Array.getLength(value1);
                int length2 = Array.getLength(value2);
                if (length1 != length2) return false;
                for (int i = 0; i < length1; i++) {
                    Object item1 = Array.get(value1, i);
                    Object item2 = Array.get(value2, i);
                    if (!match(item1, item2)) return false;
                }
                return true;
            }
        }
        // TODO C.B.: Did we mean to keep this?
        return Objects.deepEquals(value1, value2);
    }

    /**
     * To objects, value1 and value2 "match" if:
     *    - value1 and value2 are "==" including if the are both null
     *    - value1 and value2 are non-null and value1.equals(value2)
     *    - and they are both null or emptyString or the same String (C.B.?)
     *
     * @param <T>
     * @param value1 the first value
     * @param value2 the second value
     * @return true value1 and value2 match per above.
     */
    public static <T> boolean match(@Nullable T value1, @Nullable T value2) {
        if (value1 == null && value2 == null) return true;
        if (value1 == value2) return true;
        if (value1 != null && value2 != null) return value1.equals(value2);
        if (value1 instanceof String || value2 instanceof String) return Objects.equals(
                nvl(value1, ""),
                nvl(value2, ""));

        return false;
    }

    public static <T> boolean match(@Nullable T value1, @Nullable T value2, Function<T, ?> valueProvider) {
        if (value1 == null && value2 == null) return true;
        if (value1 == value2) return true;
        if (value1 != null) return match(
                valueProvider.apply(value1),
                valueProvider.apply(value2));

        return false;
    }

    public static <T> T firstOrNull(@Nullable T[] array) {
        if (array == null) return null;
        if (array.length == 0) return null;
        return array[0];
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] nonNulls(T ... array) {
        if (array == null) return null;
        List<T> nonNulls = Arrays.stream(array).filter(Objects::nonNull).collect(Collectors.toList());

        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), nonNulls.size());
        return nonNulls.toArray(result);

    }
}
