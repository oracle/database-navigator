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
import com.dbn.assistant.tool.AssistantToolInfo.UtilityDefinition;
import com.dbn.common.action.UserDataKeys;
import com.dbn.connection.ConnectionHandler;
import dev.langchain4j.agent.tool.Tool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.action.UserDataKeys.ASSISTANT_TOOL_CACHE;

@Slf4j
@Getter
public class AssistantToolCache extends AssistantStateExtension /*implements ToolProvider*/ {
    private final AssistantTool[] tools;
    private final AssistantToolType[] types;
    private final AssistantToolCategory[] categories;

    private AssistantToolCache(@NotNull AssistantState assistantState) {
        super(assistantState);
        tools = initialize(assistantState);
        types = initToolTypes(tools);
        categories = initToolCategories(tools);
    }

    private AssistantToolCategory[] initToolCategories(AssistantTool[] tools) {
        return Arrays
                .stream(tools)
                .map(t -> t.getCategory())
                .distinct()
                .toArray(l -> new AssistantToolCategory[l]);
    }

    private AssistantToolType[] initToolTypes(AssistantTool[] tools) {
        return Arrays
                .stream(tools)
                .map(t -> t.getType())
                .distinct()
                .toArray(l -> new AssistantToolType[l]);
    }

    private static AssistantTool[] initialize(AssistantState assistantState) {
        List<AssistantTool> tools = new ArrayList<>();
        List<AssistantToolFactory> factories = AssistantToolFactories.list();
        for (AssistantToolFactory factory : factories) {
            try {
                ConnectionHandler connection = assistantState.getConnection();
                AssistantTool tool = factory.createTool(connection);
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

        return tools.toArray(new AssistantTool[0]);
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
            Tool utility = getUtility(tool, utilityName);
            if (utility != null) return tool;
        }
        return null;
    }

    @Nullable
    private static Tool getUtility(AssistantTool tool, String utilityName) {
        Method[] methods = getSpecification(tool).getDeclaredMethods();
        for (Method method : methods) {
            Tool t = method.getAnnotation(Tool.class);
            if (t == null) continue;
            if (t.name().equals(utilityName)) return t;
        }

        return null;
    }

    @Nullable
    public static UtilityDefinition getUtilityDefinition(AssistantTool tool, String name) {
        Method[] methods = getSpecification(tool).getDeclaredMethods();
        for (Method method : methods) {
            Tool t = method.getAnnotation(Tool.class);
            if (t == null) continue;
            if (t.name().equals(name)) return method.getAnnotation(UtilityDefinition.class);
        }

        return null;
    }

/*    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        return null;
    }*/
}
