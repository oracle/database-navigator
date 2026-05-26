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

package com.dbn.assistant.tool.execution;

import dev.langchain4j.agent.tool.P;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.Reflection.getParameterAnnotations;

@UtilityClass
public class AssistantToolParameters {
    private static final Map<Method, Map<String, AssistantToolParameter>> REGISTRY = new ConcurrentHashMap<>();

    public static AssistantToolParameter getToolParameter(Method method, int index) {
        Map<String, AssistantToolParameter> toolParameters = getToolParameters(method);
        return toolParameters.get(getParameterName(index));
    }

    public static Map<String, AssistantToolParameter> getToolParameters(Method method) {
        return REGISTRY.computeIfAbsent(method, m -> loadToolParameter(m));
    }

    private static Map<String, AssistantToolParameter> loadToolParameter(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        P[] parameterAnnotations = getParameterAnnotations(method, P.class);
        Map<String, AssistantToolParameter> map = new LinkedHashMap<>();
        for (int i = 0; i < parameterTypes.length; i++) {
            String parameterName = getParameterName(i);
            Class<?> parameterType = parameterTypes[i];
            P parameterAnnotation = parameterAnnotations[i];
            AssistantToolParameter parameterInfo = new AssistantToolParameter(parameterName, parameterType, parameterAnnotation.required());
            map.put(parameterName, parameterInfo);
        }
        return map;
    }

    private static @NotNull String getParameterName(int index) {
        return "arg" + index;
    }

}
