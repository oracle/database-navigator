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

package com.dbn.assistant.tool.event;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.common.component.ConnectionComponent;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;

import static com.dbn.assistant.tool.event.AssistantToolEventType.CANCELLED;
import static com.dbn.assistant.tool.event.AssistantToolEventType.COMPLETED;
import static com.dbn.assistant.tool.event.AssistantToolEventType.FAILED;
import static com.dbn.assistant.tool.event.AssistantToolEventType.REQUESTED;

public class AssistantToolInvocationHandler<T extends AssistantTool> extends ConnectionComponent implements InvocationHandler {
    private final T tool;

    public AssistantToolInvocationHandler(@NotNull ConnectionHandler connection, T tool) {
        super(connection);
        this.tool = tool;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();
        try {
            notifyEvent(project, REQUESTED, tool, method, null);
            Object result = invokeMethod(method, args);
            notifyEvent(project, COMPLETED, tool, method, null);
            return result;
        } catch (ProcessCanceledException t) {
            notifyEvent(project, CANCELLED, tool, method, null);
            throw new CancellationException("Cancelled by user");
        } catch (Throwable t) {
            Throwable exception = Exceptions.unwrap(t);
            notifyEvent(project, FAILED, tool, method, exception);
            throw exception;
        }
    }

    @SneakyThrows
    private Object invokeMethod(Method method, Object[] args) {
        try {
            return method.invoke(tool, args);
        } catch (Throwable e) {
            throw Exceptions.unwrap(e);
        }
    }

    private static <T extends AssistantTool> void notifyEvent(Project project, AssistantToolEventType type, T tool, Method method, Throwable exception) {
        AssistantToolEvent event = type.createEvent(tool, method);
        event.setException(exception);
        ProjectEvents.notify(project, AssistantToolListener.TOPIC, l -> l.processEvent(event));
    }
}
