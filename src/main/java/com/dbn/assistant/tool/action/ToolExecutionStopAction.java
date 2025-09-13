/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.action;

import com.dbn.assistant.chat.message.ui.ChatMessageToolSectionForm;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.icon.Icons.ACTION_STOP;

/**
 * Action for starting a new chat
 */
public class ToolExecutionStopAction extends AssistantToolAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatMessageToolSectionForm toolSectionForm = getToolSectionForm(e);
        if (toolSectionForm == null) return;

        toolSectionForm.cancelToolExecution();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(enabled);
        presentation.setText("Stop Execution");
        presentation.setIcon(ACTION_STOP);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatMessageToolSectionForm toolSectionForm = getToolSectionForm(e);
        if (toolSectionForm == null) return false;

        AssistantToolInvocation invocation = toolSectionForm.getToolInvocation();
        return invocation.getStatus() == AssistantToolStatus.EXECUTING;
    }
}
