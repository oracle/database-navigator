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

package com.dbn.assistant.chat.window.action;

import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Messages;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Action for clearing the AI Assistant chat history
 *
 * @author Dan Cioca (Oracle)
 */
public class ChatDeleteAction extends AbstractChatBoxAction {
    public ChatDeleteAction() {
        super(txt("app.assistant.action.AssistantClearChat"));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);
        boolean persisted = isPersisted(e);

        String text = persisted ?
                txt("app.assistant.action.DeleteChat") :
                txt("app.assistant.action.ClearChat");

        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_DELETE);
        presentation.setText(text);
        presentation.setEnabled(enabled);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        if (!chatBox.hasMessages(AuthorType.AGENT)) {
            // delete without confirmation
            chatBox.deleteCurrentChat();
            return;
        }

        boolean persisted = isPersisted(e);

        String title = persisted ?
                txt("msg.assistant.title.DeleteChat") :
                txt("msg.assistant.title.ClearChat");

        String message = persisted ?
                txt("msg.assistant.question.DeleteChat") :
                txt("msg.assistant.question.ClearChat");

        showQuestionDialog(project, title, message,
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () -> chatBox.deleteCurrentChat()));

    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        AssistantState state = getAssistantState(e);
        if (state == null) return false;

        Chat chat = state.getCurrentChat();
        if (chat.isEmpty()) return false;

        return true;
    }

    private boolean isPersisted(@NotNull AnActionEvent e) {
        AssistantState state = getAssistantState(e);
        if (state == null) return false;

        Chat chat = state.getCurrentChat();
        return chat.isPersisted();
    }

}
