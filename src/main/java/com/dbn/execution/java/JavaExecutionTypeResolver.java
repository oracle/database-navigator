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

package com.dbn.execution.java;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

@UtilityClass
public final class JavaExecutionTypeResolver {
    private static final Map<String, Class<?>> INPUT_TYPES = createInputTypes();

    private static Map<String, Class<?>> createInputTypes() {
        @NonNls
        Map<String, Class<?>> types = new HashMap<>();
        types.put("boolean", boolean.class);
        types.put("byte", byte.class);
        types.put("char", char.class);
        types.put("short", short.class);
        types.put("int", int.class);
        types.put("long", long.class);
        types.put("float", float.class);
        types.put("double", double.class);

        types.put(Boolean.class.getCanonicalName(), Boolean.class);
        types.put(Byte.class.getCanonicalName(), Byte.class);
        types.put(Character.class.getCanonicalName(), Character.class);
        types.put(Short.class.getCanonicalName(), Short.class);
        types.put(Integer.class.getCanonicalName(), Integer.class);
        types.put(Long.class.getCanonicalName(), Long.class);
        types.put(Float.class.getCanonicalName(), Float.class);
        types.put(Double.class.getCanonicalName(), Double.class);
        types.put(String.class.getCanonicalName(), String.class);
        types.put(BigDecimal.class.getCanonicalName(), BigDecimal.class);
        types.put(BigInteger.class.getCanonicalName(), BigInteger.class);

        return unmodifiableMap(types);
    }

    public static @NotNull Class<?> resolveInputType(@Nullable @NonNls String className) {
        Class<?> type = INPUT_TYPES.get(className);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported Java execution parameter type: " + className);
        }
        return type;
    }

    public static boolean isInputTypeSupported(@Nullable @NonNls String className) {
        return INPUT_TYPES.containsKey(className);
    }
}
