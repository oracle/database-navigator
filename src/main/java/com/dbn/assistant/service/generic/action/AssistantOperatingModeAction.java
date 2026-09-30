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

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.SelectDropdownAction;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.assistant.AssistantMode.ANALYTICS;
import static com.dbn.assistant.AssistantMode.DEVELOPMENT;
import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.database.DatabaseFeature.VECTOR_SEARCH;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.options.general.WorkspaceFeature.VECTOR_TOOLBOX;

public class AssistantOperatingModeAction extends SelectDropdownAction<AssistantMode> implements AssistantActionSupport {

    public AssistantOperatingModeAction() {
        super(txt("app.assistant.action.AssistantOperatingMode"));
    }

    @Override
    protected @Nullable List<AssistantMode> getObjects(DataContext dataContext) {
        AssistantState assistantState = getAssistantState(dataContext);
        if (assistantState == null) return null;

        ConnectionHandler connection = assistantState.getConnection();
        Project project = connection.getProject();
        if (VECTOR_TOOLBOX.isEnabled(project) &&
                VECTOR_SEARCH.isSupported(connection)) {
            return Arrays.asList(AssistantMode.values());
        } else {
            return List.of(DEVELOPMENT, ANALYTICS);
        }
    }

    @Override
    protected AssistantMode getSelectedObject(AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return DEVELOPMENT;

        return chatContext.getAssistantMode();
    }

    @Override
    protected void setSelectedObject(AnActionEvent e, AssistantMode object) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return;

        chatContext.setAssistantMode(object);
    }

    protected boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    @Override
    protected String getDescription(AnActionEvent e) {
        return txt("app.assistant.tooltip.OperatingMode");
    }

    @Override
    protected String getDescription(AssistantMode element) {
        return element.getDescription();
    }
}
