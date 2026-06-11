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

package com.dbn.assistant.tool;

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import dev.langchain4j.agent.tool.Tool;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.assistant.tool.AssistantToolType.SUPPORT;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.common.util.Unsafe.silent;
import static com.dbn.nls.NlsResources.txtOr;

public class AssistantToolData {
    private static final @NonNls String TOOL_TYPE_NAME_KEY = "app.assistant.label.ToolTypeName_";
    private static final @NonNls String TOOL_TYPE_DESCRIPTION_KEY = "app.assistant.text.ToolTypeDescription_";
    private static final @NonNls String TOOL_UTILITY_NAME_KEY = "app.assistant.label.ToolUtilityName_";
    private static final @NonNls String TOOL_UTILITY_DESCRIPTION_KEY = "app.assistant.text.ToolUtilityDescription_";

    private static final List<AssistantToolFactory> factories = AssistantToolFactories.list();
    private static final List<AssistantToolCategory> categories = categories();
    private static final Map<String, AssistantToolFactory<?>> utilityMappings = new ConcurrentHashMap<>();
    private static final Map<AssistantToolType, AssistantToolFactory> typeMappings = factories();

    private static @NotNull List<AssistantToolCategory> categories() {
        return factories
                .stream()
                .map(t -> t.getToolCategory())
                .distinct()
                .toList();
    }

    private static Map<AssistantToolType, AssistantToolFactory> factories() {
        return factories.stream().collect(Collectors.toMap(AssistantToolFactory::getToolType, f -> f));
    }

    public static List<AssistantToolCategory> getToolCategories() {
        return categories;
    }

    public static List<AssistantToolCategory> getSupportedToolCategories(AssistantState assistantState) {
        return filter(getToolCategories(), c -> isSupported(c, assistantState));
    }

    public static List<AssistantToolType> getToolTypes(@Nullable AssistantToolCategory category) {
        return factories
                .stream()
                .filter(t -> category == null || t.getToolCategory() == category)
                .map(t -> t.getToolType())
                .toList();
    }

    public static List<AssistantToolType> getSupportedToolTypes(AssistantState assistantState, @Nullable AssistantToolCategory category) {
        List<AssistantToolType> toolTypes = getToolTypes(category);

        return filter(toolTypes, t -> isSupported(t, assistantState));
    }

    public static boolean isSupported(AssistantToolType toolType, AssistantState assistantState) {
        DatabaseCompatibilityInterface compatibility = assistantState.getConnection().getCompatibilityInterface();
        if (!compatibility.isAssistantToolSupported(toolType)) return false;

        AssistantToolCategory toolCategory = getToolCategory(toolType);
        if (!compatibility.isAssistantToolSupported(toolCategory)) return false;

        AssistantMode assistantMode = assistantState.getAssistantMode();
        return SUPPORT.get(assistantMode).contains(toolType);
    }

    public static boolean isSupported(AssistantToolCategory toolCategory, AssistantState assistantState) {
        DatabaseCompatibilityInterface compatibility = assistantState.getConnection().getCompatibilityInterface();
        if (!compatibility.isAssistantToolSupported(toolCategory)) return false;

        List<AssistantToolType> toolTypes = getToolTypes(toolCategory);
        return toolTypes.stream().anyMatch(t -> isSupported(t, assistantState));
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

    public static boolean isInternalTool(String utilityName) {
        return silent(null, () -> getUtilityMethod(utilityName)) != null;
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
        if (ExternalAssistantTool.class.isAssignableFrom(toolClass)) return toolClass;

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

    public static @Nls String getToolDisplayName(AssistantTool tool) {
        return txtOr(TOOL_TYPE_NAME_KEY + tool.getType(), tool.getName());
    }

    public static @Nls String getToolDisplayDescription(AssistantTool tool) {
        return txtOr(TOOL_TYPE_DESCRIPTION_KEY + tool.getType(), tool.getDescription());
    }

    public static @Nls String getToolDisplayName(AssistantToolType toolType) {
        String toolName = getToolName(toolType);
        return txtOr(TOOL_TYPE_NAME_KEY + toolType, toolName);
    }

    public static @Nls String getToolDisplayDescription(AssistantToolType toolType) {
        String toolDescription = getToolDescription(toolType);
        return txtOr(TOOL_TYPE_DESCRIPTION_KEY + toolType, toolDescription);
    }

    public static @Nls String getUtilityDisplayName(AssistantTool tool, String utilityName) {
        UtilitySpec utilitySpec = getUtilitySpec(tool, utilityName);
        String fallback = utilitySpec == null ? utilityName : utilitySpec.name();
        return txtOr(TOOL_UTILITY_NAME_KEY + utilityName, fallback);
    }

    public static @Nls String getUtilityDisplayDescription(AssistantTool tool, String utilityName) {
        UtilitySpec utilitySpec = getUtilitySpec(tool, utilityName);
        String fallback = utilitySpec == null ? "" : utilitySpec.description();
        return txtOr(TOOL_UTILITY_DESCRIPTION_KEY + utilityName, fallback);
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
