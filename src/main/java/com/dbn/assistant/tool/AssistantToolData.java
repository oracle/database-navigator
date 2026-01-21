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

package com.dbn.assistant.tool;

import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import dev.langchain4j.agent.tool.Tool;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.util.Unsafe.cast;

public class AssistantToolData {
    private static final List<AssistantToolFactory> factories = AssistantToolFactories.list();
    private static final Map<String, AssistantToolFactory<?>> utilityMappings = new ConcurrentHashMap<>();
    private static final Map<AssistantToolType, AssistantToolFactory> typeMappings = factories();


    private static Map<AssistantToolType, AssistantToolFactory> factories() {
        return factories.stream().collect(Collectors.toMap(AssistantToolFactory::getToolType, f -> f));
    }

    public static AssistantToolCategory[] getToolCategories() {
        return factories
                .stream()
                .map(t -> t.getToolCategory())
                .distinct()
                .toArray(AssistantToolCategory[]::new);
    }

    public static List<AssistantToolType> getToolTypes(@Nullable AssistantToolCategory category) {
        return factories
                .stream()
                .filter(t -> category == null || t.getToolCategory() == category)
                .map(t -> t.getToolType())
                .toList();
    }

    public static boolean isInteractiveTool(String utilityName) {
        return getToolFactory(utilityName).isInteractive();
    }

    public static AssistantToolType getToolType(String utilityName) {
        return getToolFactory(utilityName).getToolType();
    }

    public static AssistantToolCategory getToolCategory(String utilityName) {
        return getToolFactory(utilityName).getToolCategory();
    }

    private static <T extends AssistantTool> AssistantToolFactory<T> getToolFactory(String utilityName) {
        return cast(utilityMappings.computeIfAbsent(utilityName, n -> findToolFactory(n)));
    }

    private static <T extends AssistantTool> AssistantToolFactory<T> findToolFactory(String utilityName) {
        for (AssistantToolFactory factory : factories) {
            Class<? extends AssistantTool> toolSpecification = factory.getToolSpecification();
            UtilitySpec utilitySpec = getUtilitySpec(toolSpecification, utilityName);
            if (utilitySpec != null) return cast(factory);
        }
        throw new IllegalArgumentException("No AssistantToolFactory found for " + utilityName);
    }

    public static Method getUtilityMethod(String utilityName) {
        Class<? extends AssistantTool> toolSpecification = getToolFactory(utilityName).getToolSpecification();
        return getUtilityMethod(toolSpecification, utilityName);
    }

    @Nullable
    public static Method getUtilityMethod(Class<? extends AssistantTool> toolClass, String utilityName) {
        Method[] methods = getToolSpecification(toolClass).getDeclaredMethods();
        for (Method method : methods) {
            Tool t = method.getAnnotation(Tool.class);
            if (t == null) continue;
            if (t.name().equals(utilityName)) return method;
        }

        return null;
    }

    public static Class getToolSpecification(Class<? extends AssistantTool> toolClass) {
        if (toolClass.isInterface() && AssistantTool.class.isAssignableFrom(toolClass)) return toolClass;

        Class<?>[] interfaces = toolClass.getInterfaces();
        for (Class<?> spec : interfaces) {
            if (AssistantTool.class.isAssignableFrom(spec)) return spec;
        }

        throw new IllegalArgumentException("Class " + toolClass.getName() + " does not implement " + AssistantTool.class.getName());
    }

    @Nullable
    public static UtilitySpec getUtilitySpec(AssistantTool tool, String utilityName) {
        return getUtilitySpec(tool.getClass(), utilityName);
    }

    @Nullable
    public static UtilitySpec getUtilitySpec(Class<? extends AssistantTool> toolClass, String utilityName) {
        Method method = getUtilityMethod(toolClass, utilityName);
        if (method == null) return null;

        return method.getAnnotation(UtilitySpec.class);
    }

    public static String getToolName(AssistantToolType toolType) {
        AssistantToolFactory factory = typeMappings.get(toolType);
        return factory.getToolName();
    }

    public static String getToolDescription(AssistantToolType toolType) {
        AssistantToolFactory factory = typeMappings.get(toolType);
        return factory.getToolDescription();
    }

    public static AssistantToolCategory getToolCategory(AssistantToolType toolType) {
        AssistantToolFactory factory = typeMappings.get(toolType);
        return factory.getToolCategory();
    }

    public static boolean isInteractive(AssistantToolType toolType) {
        AssistantToolFactory factory = typeMappings.get(toolType);
        return factory.isInteractive();
    }
}
