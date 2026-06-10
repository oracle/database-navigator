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

package com.dbn.assistant.tool.feature;

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class AssistantToolFeatureContext {
    private final AssistantToolRequest toolRequest;
    private final ChatContext chatContext;
    private final AssistantState assistantState;
    private final Runnable onApprove;
    private final Runnable onDeny;

    public AssistantToolFeatureContext(
            @NotNull AssistantToolRequest toolRequest,
            @NotNull ChatContext chatContext,
            @NotNull AssistantState assistantState) {
        this(toolRequest, chatContext, assistantState, () -> {}, () -> {});
    }

    public AssistantToolFeatureContext(
            @NotNull AssistantToolRequest toolRequest,
            @NotNull ChatContext chatContext,
            @NotNull AssistantState assistantState,
            @NotNull Runnable onApprove,
            @NotNull Runnable onDeny) {
        this.toolRequest = toolRequest;
        this.chatContext = chatContext;
        this.assistantState = assistantState;
        this.onApprove = onApprove;
        this.onDeny = onDeny;
    }

    public void approve() {
        onApprove.run();
    }

    public void deny() {
        onDeny.run();
    }

    public ConnectionHandler getConnection() {
        return assistantState.getConnection();
    }

    public Project getProject() {
        return getConnection().getProject();
    }
}
