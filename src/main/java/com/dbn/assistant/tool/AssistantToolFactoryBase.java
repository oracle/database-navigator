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
import com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.event.AssistantToolInvocationHandler;
import com.dbn.connection.ConnectionHandler;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static com.dbn.common.util.Unsafe.cast;

public abstract class AssistantToolFactoryBase<T extends AssistantTool> implements AssistantToolFactory<T> {
    private final FactorySpec factorySpec;
    private final ToolSpec toolSpec;

    public AssistantToolFactoryBase() {
        factorySpec = getClass().getAnnotation(FactorySpec.class);
        if (factorySpec == null) throw new NullPointerException("Missing @AssistantTool.FactorySpec annotation");

        Class<T> spec = getToolSpecification();
        Class<T> impl = getToolImplementation();
        if (!spec.isAssignableFrom(impl)) throw new ClassCastException("Specified interface is not assignable to given implementation");

        toolSpec = spec.getAnnotation(ToolSpec.class);
        if (toolSpec == null) throw new NullPointerException("Missing @AssistantTool.ToolSpec annotation");
    }

    @Override
    public AssistantToolType getToolType() {
        return toolSpec.type();
    }

    @Override
    public AssistantToolCategory getToolCategory() {
        return toolSpec.category();
    }

    @Override
    public String getToolName() {
        return toolSpec.name();
    }

    @Override
    public String getToolDescription() {
        return toolSpec.description();
    }

    @Override
    public boolean isInteractive() {
        return toolSpec.interactive();
    }

    @Override
    public Class<T> getToolSpecification() {
        return cast(factorySpec.spec());
    }

    @Override
    public Class<T> getToolImplementation() {
        return cast(factorySpec.impl());
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
                getToolCategory(),
                isInteractive());

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
