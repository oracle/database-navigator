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
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.ProjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract basic implementation for actions presented in the ChatBox
 * Features lookup utilities for the chat box component as well as the state of the assistant interface
 *
 * @author Dan Cioca (Oracle)
 */
@BackgroundUpdate
public abstract class AssistantToolAction extends ProjectAction implements AssistantActionSupport {

    @Nullable
    protected ChatMessageToolSectionForm getToolSectionForm(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.CHAT_MESSAGE_TOOL_SECTION_FORM);
    }

    protected boolean isInteractive(@NotNull AnActionEvent e) {
        ChatMessageToolSectionForm toolSectionForm = getToolSectionForm(e);
        if (toolSectionForm == null) return false;

        return toolSectionForm.isInteractiveTool();
    }
}
