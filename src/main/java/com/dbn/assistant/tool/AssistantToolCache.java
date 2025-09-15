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

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.approval.AssistantToolFilter;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.list.FilteredList;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dbn.common.action.UserDataKeys.ASSISTANT_TOOL_CACHE;

@Slf4j
public class AssistantToolCache extends AssistantStateExtension implements ToolProvider {
    private final List<AssistantTool> tools;
    private final ToolProvider provider = new AssistantToolProvider(this);

    private AssistantToolCache(@NotNull AssistantState assistantState) {
        super(assistantState);
        List<AssistantTool> tools = initTools(assistantState);

        AssistantToolSettings settings = AssistantToolSettings.get(assistantState);
        AssistantToolApprovals approvals = settings.getApprovals();

        AssistantToolFilter filter = new AssistantToolFilter(approvals);
        this.tools = FilteredList.stateful(filter, tools);
    }

    private static List<AssistantTool> initTools(AssistantState assistantState) {
        List<AssistantTool> tools = new ArrayList<>();
        List<AssistantToolFactory> factories = AssistantToolFactories.list();
        for (AssistantToolFactory factory : factories) {
            try {
                AssistantTool tool = factory.createTool(assistantState);
                tools.add(tool);
            } catch (Throwable e) {
                log.error("Failed to create {} assistant tool of type {} (spec={} impl={})",
                        factory.getToolCategory(),
                        factory.getToolType(),
                        factory.getToolSpecification(),
                        factory.getToolImplementation(),
                        e);
            }
        }

        return tools;
    }

    public static AssistantToolCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_TOOL_CACHE, () -> new AssistantToolCache(assistantState));
    }

    private static Class getSpecification(AssistantTool tool) {
        Class<?>[] interfaces = tool.getClass().getInterfaces();
        for (Class<?> spec : interfaces) {
            if (AssistantTool.class.isAssignableFrom(spec)) return spec;
        }

        throw new IllegalArgumentException("Class " + tool.getClass().getName() + " does not implement " + AssistantTool.class.getName());
    }

    @Nullable
    public AssistantTool getAssistantTool(String utilityName) {
        for (AssistantTool tool : tools) {
            UtilitySpec utilitySpec = getUtilitySpec(tool, utilityName);
            if (utilitySpec != null) return tool;
        }
        return null;
    }

    @Nullable
    public AssistantTool getAssistantTool(AssistantToolType toolType) {
        for (AssistantTool tool : tools) {
            if (Objects.equals(tool.getType(), toolType)) return tool;
        }
        return null;
    }

    @Nullable
    public static Method getUtilityMethod(AssistantTool tool, String utilityName) {
        Method[] methods = getSpecification(tool).getDeclaredMethods();
        for (Method method : methods) {
            Tool t = method.getAnnotation(Tool.class);
            if (t == null) continue;
            if (t.name().equals(utilityName)) return method;
        }

        return null;
    }

    @Nullable
    public static UtilitySpec getUtilitySpec(AssistantTool tool, String utilityName) {
        Method method = getUtilityMethod(tool, utilityName);
        if (method == null) return null;

        return method.getAnnotation(UtilitySpec.class);
    }

    public AssistantTool[] getAvailableTools() {
        return tools.toArray(new AssistantTool[0]);
    }

    public AssistantToolCategory[] getToolCategories() {
        return FilteredList.unwrap(tools)
                .stream()
                .map(t -> t.getCategory())
                .distinct()
                .toArray(AssistantToolCategory[]::new);
    }
    public AssistantToolCategory[] getAvailableToolCategories() {
        AssistantTool[] tools = getAvailableTools();
        return Arrays
                .stream(tools)
                .map(t -> t.getCategory())
                .distinct()
                .toArray(AssistantToolCategory[]::new);
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        return provider.provideTools(request);
    }

    public List<AssistantToolType> getToolTypes(AssistantToolCategory category) {
        return FilteredList
                .unwrap(tools)
                .stream()
                .filter(t -> t.getCategory() == category)
                .map(t -> t.getType())
                .collect(Collectors.toList());
    }
}
