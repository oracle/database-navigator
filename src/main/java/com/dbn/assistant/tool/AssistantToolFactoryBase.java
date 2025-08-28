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

import com.dbn.connection.ConnectionHandler;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;

import static com.dbn.common.util.Unsafe.cast;

@Getter
public abstract class AssistantToolFactoryBase<T extends AssistantTool> implements AssistantToolFactory<T> {
    private final AssistantToolCategory toolCategory;
    private final AssistantToolType toolType;
    private final Class<T> toolClass;

    public AssistantToolFactoryBase() {
        Definition definition = getClass().getAnnotation(Definition.class);
        if (definition == null) throw new NullPointerException("Missing @ToolDefinition annotation");

        toolCategory = definition.category();
        toolType = AssistantToolType.get(definition.type());
        toolClass = cast(definition.impl());
    }

    @Override
    @SneakyThrows
    public final T createTool(ConnectionHandler connection) {
        Class<T> toolClass = getToolClass();
        Constructor<T> constructor = toolClass.getConstructor();
        T tool = constructor.newInstance();
        tool.initialize(connection);
        return tool;
    }
}
