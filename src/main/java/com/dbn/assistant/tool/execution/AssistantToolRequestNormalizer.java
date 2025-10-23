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

import com.dbn.assistant.tool.AssistantToolData;
import com.dbn.common.Reflection;
import com.dbn.common.data.Data;
import com.dbn.common.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.SneakyThrows;
import org.apache.xmlbeans.impl.common.Levenshtein;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.dbn.assistant.AssistantComponent.OBJECT_MAPPER;

public class AssistantToolRequestNormalizer {
    @SneakyThrows
    public static void normalize(ToolExecutionRequest request) {
        Method method = AssistantToolData.getUtilityMethod(request.name());
        if (method == null) return;

        Class<?>[] argTypes = method.getParameterTypes();
        if (argTypes.length == 0) return;

        String arguments = request.arguments();
        arguments = normalize(arguments, method);
        Reflection.updateFieldValue(request, "arguments", arguments);
    }

    @SneakyThrows
    static String normalize(String arguments, Method method){
        if (Strings.isEmptyOrSpaces(arguments)) return  "{}";

        JsonNode argumentsNode = OBJECT_MAPPER.readTree(arguments);
        if (argumentsNode instanceof TextNode) {
            // argument arrays are sometimes coming as quoted strings
            argumentsNode = OBJECT_MAPPER.readTree(argumentsNode.asText());
        }
        arguments = argumentsNode.toString();

        Map<?, ?> args = OBJECT_MAPPER.readValue(arguments, LinkedHashMap.class);
        if (args.isEmpty()) return "{}";

        Map<String, ?> normalizedArgs = normalizeArguments(args, method);
        return OBJECT_MAPPER.writeValueAsString(normalizedArgs);
    }

    static @NotNull Map<String, ?> normalizeArguments(Map<?, ?> args, Method method) {
        Map<String, ?> normalizedArgs = normalizeArgumentTypes(args);

        normalizedArgs = normalizeArgumentNames(normalizedArgs, method);
        normalizedArgs = normalizeArgumentValues(normalizedArgs, method);
        return normalizedArgs;
    }

    private static Map<String, ?> normalizeArgumentTypes(Map<?, ?> args) {
        Map<String, Object> normalizedArgs = new LinkedHashMap<>();
        args.forEach((k, v) -> normalizedArgs.put(k.toString(), v));
        return normalizedArgs;
    }

    private static Map<String, ?> normalizeArgumentNames(Map<String, ?> arguments, Method method) {
        if (isValidArgumentMap(arguments)) {
            // convert to a sorted keys map
            return new TreeMap<>(arguments);
        }

        // handle "hallucinated" arguments not matching the "arg#" format
        List<String> args = new ArrayList<>(arguments.keySet());
        Class<?>[] params = method.getParameterTypes();
        int count = Math.min(args.size(), params.length);

        Map<String, Object> normalizedArgs = new LinkedHashMap<>();
        // add arguments that match the "arg#" format
        for (int i = 0; i < count; i++) {
            String arg = args.get(i);
            if (isValidArgument(arg)) {
                Object value = arguments.get(arg);
                normalizedArgs.put(arg, value);
            }
        }

        // try to resolve arguments that don't match the "arg#" format
        for (int i = 0; i < count; i++) {
            String arg = args.get(i);
            if (isValidArgument(arg)) continue;

            Object value = arguments.get(arg);

            String probableArg = mostProbableArgumentName(method, arg);
            if (isValidArgument(probableArg) && !normalizedArgs.containsKey(probableArg)) {
                normalizedArgs.put(probableArg, value);
            } else {
                normalizedArgs.put(arg, value);
            }
        }

        if (!isValidArgumentMap(normalizedArgs)) {
            List<String> invalidArguments = normalizedArgs.keySet().stream().filter(k -> !isValidArgument(k)).collect(Collectors.toList());
            for (String invalidArgument : invalidArguments) {
                Object value = normalizedArgs.remove(invalidArgument);
                String argument = buildArgumentName(normalizedArgs.keySet());
                normalizedArgs.put(argument, value);
            }
        }

        return new TreeMap<>(normalizedArgs);
    }

    private static Map<String, Object> normalizeArgumentValues(Map<String, ?> args, Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Map<String, Object> normalizedArgs = new TreeMap<>();
        List<String> argumentKeys = new ArrayList<>(args.keySet());
        for (int i = 0; i < Math.min(argumentKeys.size(), paramTypes.length); i++) {
            String argumentName = argumentKeys.get(i);
            Object argumentValue = args.get(argumentName);
            Class<?> parameterType = paramTypes[i];

            Object normalizedValue = normalizeArgumentValue(argumentValue, parameterType);
            normalizedArgs.put(argumentName, normalizedValue);
        }
        return normalizedArgs;
    }

    private static Object normalizeArgumentValue(Object argumentValue, Class<?> parameterType) {
        if (argumentValue == null) return null;

        Class<?> argumentType = argumentValue.getClass();
        if (parameterType.isAssignableFrom(argumentType)) return argumentValue;

        // handle unsolicited quoting of booleans, numbers, lists aso..
        if (parameterType.isArray()) {
            Class<?> arrayType = parameterType.getComponentType();
            List<?> values = objectToList(argumentValue, arrayType);
            return collectionToArray(values, arrayType);
        }

        if (List.class.isAssignableFrom(parameterType)) {
            return objectToList(argumentValue, Object.class);
        }

        if (Set.class.isAssignableFrom(parameterType)) {
            return objectToSet(argumentValue, Object.class);
        }

        return Data.asType(argumentValue, parameterType);
    }

    private static Object objectToSet(Object argumentValue, Class<?> parameterType) {
        List<?> valueList = objectToList(argumentValue, parameterType);
        return new LinkedHashSet<>(valueList);
    }

    private static <T> List<T> objectToList(Object argumentValue, Class<T> elementType) {
        if (argumentValue instanceof Collection || argumentValue.getClass().isArray()) {
            return Data.asTypeList(argumentValue, elementType);
        }

        String stringValue = argumentValue.toString();
        return stringToList(stringValue, elementType);
    }

    private static <T> List<T> stringToList(String value, Class<T> arrayType) {
        try {
            List valueList = OBJECT_MAPPER.readValue(value, List.class);
            return Data.asTypeList(valueList, arrayType);
        } catch (JsonProcessingException e) {
            value = value.startsWith("[") ? value.substring(1) : value;
            value = value.endsWith("]") ? value.substring(0, value.length() - 1) : value;
            return Data.csvToList(value, arrayType);
        }
    }

    private static Object[] collectionToArray(Collection<?> typeList, Class<?> arrayType) {
        return typeList.toArray((Object[]) Array.newInstance(arrayType, 0));
    }

    private static String mostProbableArgumentName(Method method, String argumentName) {
        int index = -1;
        double maxSimilarity = 0;

        P[] annotations = Reflection.getParameterAnnotations(method, P.class);
        for (int i = 0; i < annotations.length; i++) {
            P annotation = annotations[i];
            if (annotation == null) continue;

            String paramName = simplifyArgumentName(annotation.value());
            String argName = simplifyArgumentName(argumentName);
            if (paramName.contains(argName) && argName.length() > 10) {
                // most common use-case (argument name is "hallucinated" from the parameter description)
                index = i;
                break;
            }

            // use Longest Common Subsequence (LCS) to find the most similar argument name
            int maxLength = Math.max(paramName.length(), argName.length());
            double distance = Levenshtein.distance(paramName, argName);
            double similarity = 1 - distance / maxLength;
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                index = i;
            }
        }

        if (index == -1) return argumentName;

        return buildArgumentName(index);
    }

    private static String simplifyArgumentName(String argumentName) {
        return argumentName.toLowerCase().replaceAll("[^a-zA-Z]", "");
    }

    private static String buildArgumentName(Set<String> argNames) {
        int index = 0;
        String argumentName = buildArgumentName(index);
        while (argNames.contains(argumentName)) {
            argumentName = buildArgumentName(index++);
        }
        return argumentName;
    }

    private static String buildArgumentName(int index) {
        return "arg" + index;
    }


    private static boolean isValidArgumentMap(Map<String, ?> args) {
        return args.keySet().stream().allMatch(k -> isValidArgument(k));
    }

    private static boolean isValidArgument(String argumentKey) {
        return argumentKey.matches("^arg\\d+$");
    }
}
