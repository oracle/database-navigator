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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for handling operations related to Java primitive types and their boxed counterparts.
 * Provides methods to retrieve boxed and primitive equivalents of types, check if a type is a primitive
 * or a boxed type, and determine equivalence between primitive and/or boxed types.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Primitives {
    private static final Map<Class<?>, Class<?>> primitiveToBoxed;
    private static final Map<Class<?>, Class<?>> boxedToPrimitive;

    static {
        Map<Class<?>, Class<?>> p2b = new HashMap<>();
        Map<Class<?>, Class<?>> btp = new HashMap<>();

        p2b.put(boolean.class, Boolean.class);
        p2b.put(byte.class, Byte.class);
        p2b.put(char.class, Character.class);
        p2b.put(double.class, Double.class);
        p2b.put(float.class, Float.class);
        p2b.put(int.class, Integer.class);
        p2b.put(long.class, Long.class);
        p2b.put(short.class, Short.class);
        p2b.put(void.class, Void.class);

        p2b.forEach((primitive, boxed) -> btp.put(boxed, primitive));

        primitiveToBoxed = Collections.unmodifiableMap(p2b);
        boxedToPrimitive = Collections.unmodifiableMap(btp);
    }

    @Nullable
    public static Class<?> getBoxedClass(Class<?> clazz) {
        if (isBoxed(clazz)) return clazz;
        return primitiveToBoxed.get(clazz);
    }

    @Nullable
    public static Class<?> getPrimitiveClass(Class<?> clazz) {
        if (isPrimitive(clazz)) return clazz;
        return boxedToPrimitive.get(clazz);
    }

    public static boolean isPrimitive(Class<?> clazz) {
        return primitiveToBoxed.containsKey(clazz);
    }

    public static boolean isBoxed(Class<?> clazz) {
        return boxedToPrimitive.containsKey(clazz);
    }

    public static boolean areEquivalent(Class<?> class1, Class<?> class2) {
        Class<?> primitive1 = getPrimitiveClass(class1);
        Class<?> primitive2 = getPrimitiveClass(class2);
        return primitive1 != null && primitive2 != null && Objects.equals(primitive1, primitive2);
    }
}
