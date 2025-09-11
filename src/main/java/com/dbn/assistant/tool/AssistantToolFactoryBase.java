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
import com.dbn.assistant.tool.AssistantToolInfo.FactoryDefinition;
import com.dbn.assistant.tool.AssistantToolInfo.ToolDefinition;
import com.dbn.assistant.tool.event.AssistantToolInvocationHandler;
import com.dbn.connection.ConnectionHandler;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static com.dbn.common.util.Unsafe.cast;

public abstract class AssistantToolFactoryBase<T extends AssistantTool> implements AssistantToolFactory<T> {
    private final FactoryDefinition factoryDefinition;
    private final ToolDefinition toolDefinition;

    public AssistantToolFactoryBase() {
        factoryDefinition = getClass().getAnnotation(FactoryDefinition.class);
        if (factoryDefinition == null) throw new NullPointerException("Missing @AssistantTool.FactoryDefinition annotation");

        Class<T> spec = getToolSpecification();
        toolDefinition = spec.getAnnotation(ToolDefinition.class);
        if (toolDefinition == null) throw new NullPointerException("Missing @AssistantTool.Definition annotation");
    }

    @Override
    public AssistantToolType getToolType() {
        return AssistantToolType.get(toolDefinition.type());
    }

    @Override
    public AssistantToolCategory getToolCategory() {
        return toolDefinition.category();
    }

    @Override
    public String getToolName() {
        return toolDefinition.name();
    }

    @Override
    public String getToolDescription() {
        return toolDefinition.description();
    }

    @Override
    public Class<T> getToolSpecification() {
        return cast(factoryDefinition.spec());
    }

    @Override
    public Class<T> getToolImplementation() {
        return cast(factoryDefinition.impl());
    }

    @Override
    @SneakyThrows
    public final T createTool(AssistantState assistantState) {
        Class<T> impl = getToolImplementation();
        Constructor<T> constructor = impl.getConstructor();
        T tool = constructor.newInstance();

        ConnectionHandler connection = assistantState.getConnection();
        tool.initialize(
                connection,
                getToolName(),
                getToolDescription(),
                getToolType(),
                getToolCategory());

        return proxy(assistantState, tool);
    }

    protected T proxy(AssistantState assistantState, T tool) {
        InvocationHandler invocationHandler = new AssistantToolInvocationHandler<>(assistantState, tool);
        Class<T> spec = getToolSpecification();
        return cast(Proxy.newProxyInstance(
                spec.getClassLoader(),
                new Class[]{spec},
                invocationHandler));
    }
}
