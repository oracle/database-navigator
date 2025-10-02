/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.execution;

import com.dbn.common.util.Csvs;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@UtilityClass
public class AssistantToolRequestVerifier {
    public static void verifyRequest(AssistantToolRequest request, Method method, Object... args) {
        try {
            verifyRequestMethod(request, method);
            verifyParameterNames(request);
            verifyParameterTypes(request, args);
        } catch (Throwable e) {
            log.error("Failed to verify tool request", e);
        }
    }

    private static void verifyRequestMethod(AssistantToolRequest request, Method method) {
        if (!Objects.equals(request.getMethod(), method)) {
            throw new IllegalArgumentException("The method does not match the current request");
        }
    }

    private static void verifyParameterNames(AssistantToolRequest request) {
        Class<?>[] parameterTypes = request.getMethod().getParameterTypes();
        List<String> argumentNames = request.getToolArgumentNames();

        int parameterCount = parameterTypes.length;
        int argumentCount = argumentNames.size();

        if (parameterCount != argumentCount) {
            throw new IllegalArgumentException("Tool request does not match the expected number of arguments (expected " + parameterCount + ", received " + argumentCount + ")");
        }
        IntStream.range(0, parameterCount).mapToObj(i -> "arg" + i).forEach(n -> argumentNames.remove(n));
        if (!argumentNames.isEmpty()) {
            String unknownArgs = Csvs.stringsToCsv(argumentNames);
            throw new IllegalArgumentException("Tool request does not match the expected argument names (unknown arguments: " + unknownArgs + "). Check tool specifications.");
        }
    }

    private static void verifyParameterTypes(AssistantToolRequest request, Object[] args) {
        Class<?>[] parameterTypes = request.getMethod().getParameterTypes();

        int parameterCount = parameterTypes.length;
        int argumentCount = args == null ? 0 : args.length;

        if (parameterCount != argumentCount) {
            throw new IllegalArgumentException("Tool request does not match the expected number of arguments (expected " + parameterCount + ", received " + argumentCount + ")");
        }
        for (int i = 0; i < parameterCount; i++) {
            Class expectedType = normalizeClass(parameterTypes[i]);
            Class receivedType = normalizeClass(args[i]);
            if (!Objects.equals(expectedType, receivedType)) {
                throw new IllegalArgumentException("Tool request does not match the expected argument type at index " + i + " (" + expectedType + ", received " + receivedType + ")");
            }
        }
    }

    private static Class normalizeClass(Class clazz) {
        if (int.class.equals(clazz)) return Number.class;
        if (byte.class.equals(clazz)) return Number.class;
        if (long.class.equals(clazz)) return Number.class;
        if (short.class.equals(clazz)) return Number.class;
        if (float.class.equals(clazz)) return Number.class;
        if (double.class.equals(clazz)) return Number.class;
        if (boolean.class.equals(clazz)) return Boolean.class;
        if (char.class.equals(clazz)) return Character.class;
        if (Number.class.isAssignableFrom(clazz)) return Number.class;
        if (Boolean.class.isAssignableFrom(clazz)) return Boolean.class;
        if (Character.class.isAssignableFrom(clazz)) return Character.class;
        if (CharSequence.class.isAssignableFrom(clazz)) return CharSequence.class;
        if (Collection.class.isAssignableFrom(clazz)) return Collection.class;
        // add more cases if tool parameter types go beyond scalar

        return clazz;
    }

    private static Class normalizeClass(Object object) {
        if (object == null) return Object.class;
        if (object instanceof Number) return Number.class;
        if (object instanceof Boolean) return Boolean.class;
        if (object instanceof Character) return Character.class;
        if (object instanceof CharSequence) return CharSequence.class;
        if (object instanceof Collection) return Collection.class;
        // add more cases if tool parameter types go beyond scalar

        return object.getClass();
    }
}
