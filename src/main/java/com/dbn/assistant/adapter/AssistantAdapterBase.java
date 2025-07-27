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

package com.dbn.assistant.adapter;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.state.AssistantState;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class AssistantAdapterBase implements AssistantAdapter {
    private final AssistantType assistantType;

    public AssistantAdapterBase(AssistantType assistantType) {
        this.assistantType = assistantType;
    }

    @Nullable
    protected AssistantState getAssistantState(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return null;

        Project project = connection.getProject();
        DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);

        return manager.getAssistantState(connectionId, assistantType);
    }

    @Nullable
    protected ChatContext getChatContext(ConnectionId connectionId) {
        AssistantState assistantState = getAssistantState(connectionId);
        return assistantState == null ? null : assistantState.getCurrentContext();
    }

    @Override
    public String buildChatContextTitle(ChatContext context) {
        return context.getProvider().getName() + " / " + context.getModel().getName();
    }

    @Override
    public ChatContext enrichChatContext(ChatContext context) {
        return context;
    }

    @Override
    public final void generate(String prompt, ConnectionId connectionId, ChatContext chatContext, AssistantResponseConsumer responseConsumer) {
        try {

            String message = generate(prompt, connectionId, chatContext);
            responseConsumer.acceptMessage(message);
        } catch (Throwable t) {
            Diagnostics.conditionallyLog(t);
            responseConsumer.acceptError(t);
        } finally {
            responseConsumer.acceptCompletion();
        }
    }

    public abstract String generate(String prompt, ConnectionId connectionId, ChatContext chatContext) throws Exception;
}
