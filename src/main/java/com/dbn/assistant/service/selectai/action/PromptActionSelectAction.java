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

package com.dbn.assistant.service.selectai.action;

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.service.selectai.PromptAction;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.service.selectai.PromptAction.CHAT;
import static com.dbn.assistant.service.selectai.PromptAction.EXPLAIN_SQL;
import static com.dbn.assistant.service.selectai.PromptAction.NARRATE;
import static com.dbn.assistant.service.selectai.PromptAction.SHOW_SQL;
import static com.dbn.assistant.service.selectai.SelectAiContextUtil.getSelectedAction;
import static com.dbn.nls.NlsResources.txt;
//import static com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR;

/**
 * Action for selecting the type of interaction with the AI-assistant engine
 *
 * @author Dan Cioca (Oracle)
 */
@BackgroundUpdate
public class PromptActionSelectAction extends ToggleAction implements AssistantActionSupport {
    private final PromptAction action;

    PromptActionSelectAction(PromptAction action) {
        super(action.getName(), action.getDescription(), null);
        this.action = action;
        getTemplatePresentation().setIcon(null);

        // TODO only supported in 2024.x or higher
        //getTemplatePresentation().putClientProperty(SHOW_TEXT_IN_TOOLBAR, true);
    }

    PromptActionSelectAction(PromptAction action, String text) {
        super(text, action.getDescription(), null);
        this.action = action;
        getTemplatePresentation().setIcon(null);

        // TODO only supported in 2024.x or higher
        //getTemplatePresentation().putClientProperty(SHOW_TEXT_IN_TOOLBAR, true);
    }

    @Override
    @Compatibility
    public boolean displayTextInToolbar() {
        return true;
    }

    public static class ShowSQL extends PromptActionSelectAction {
        public ShowSQL() {
            super(SHOW_SQL, txt("app.assistant.action.AssistantShowSql"));
        }
    }

    public static class ExplainSQL extends PromptActionSelectAction {
        public ExplainSQL() {
            super(EXPLAIN_SQL, txt("app.assistant.action.AssistantExplainSql"));
        }
    }

    public static class Narrate extends PromptActionSelectAction {
        public Narrate() {
            super(NARRATE, txt("app.assistant.action.AssistantNarrate"));
        }
    }

    public static class Chat extends PromptActionSelectAction {
        public Chat() {
            super(CHAT, txt("app.assistant.action.AssistantChat"));
        }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return false;

        ConnectionId connectionId = chatBox.getConnectionId();
        PromptAction action = getSelectedAction(connectionId);
        return this.action == action;
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        if (!selected) return;

        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        // preserve profile and model from the current context
        ChatContext currentContext = chatBox.getCurrentContext();
        ChatContext targetContext = new ChatContextImpl(
                currentContext.getAssistantType(),
                currentContext.getAssistantMode(),
                currentContext.getProfileId(),
                currentContext.getProviderId(),
                currentContext.getModelId(),
                action.getId(),
                currentContext.getEmbeddingTable(),
                currentContext.isInteractive());

        chatBox.attemptContextSwitch(targetContext);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        boolean enabled = isEnabled(e);
        Presentation presentation = e.getPresentation();
        presentation.setText(action.getName());
        presentation.setEnabled(enabled);
    }
}
