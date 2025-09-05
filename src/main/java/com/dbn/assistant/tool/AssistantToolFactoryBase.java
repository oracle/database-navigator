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

import com.dbn.assistant.tool.AssistantToolInfo.Definition;
import com.dbn.assistant.tool.AssistantToolInfo.FactoryDefinition;
import com.dbn.assistant.tool.event.AssistantToolInvocationHandler;
import com.dbn.connection.ConnectionHandler;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static com.dbn.common.util.Unsafe.cast;

public abstract class AssistantToolFactoryBase<T extends AssistantTool> implements AssistantToolFactory<T> {
    private final FactoryDefinition factoryDefinition;
    private final Definition definition;

    public AssistantToolFactoryBase() {
        factoryDefinition = getClass().getAnnotation(FactoryDefinition.class);
        if (factoryDefinition == null) throw new NullPointerException("Missing @AssistantTool.FactoryDefinition annotation");

        Class<T> spec = getToolSpecification();
        definition = spec.getAnnotation(Definition.class);
        if (definition == null) throw new NullPointerException("Missing @AssistantTool.Definition annotation");
    }

    @Override
    public AssistantToolType getToolType() {
        return AssistantToolType.get(definition.type());
    }

    @Override
    public AssistantToolCategory getToolCategory() {
        return definition.category();
    }

    @Override
    public String getToolDescription() {
        return definition.description();
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
    public final T createTool(ConnectionHandler connection) {
        Class<T> impl = getToolImplementation();
        Constructor<T> constructor = impl.getConstructor();
        T tool = constructor.newInstance();

        tool.initialize(
                connection,
                getToolType(),
                getToolCategory(),
                getToolDescription());
        return proxy(tool);
    }

    protected T proxy(T tool) {
        ConnectionHandler connection = tool.getConnection();
        InvocationHandler invocationHandler = new AssistantToolInvocationHandler<>(connection, tool);
        Class<T> spec = getToolSpecification();
        return cast(Proxy.newProxyInstance(
                spec.getClassLoader(),
                new Class[]{spec},
                invocationHandler));
    }
}
