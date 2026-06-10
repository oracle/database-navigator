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

package com.dbn.assistant.chat.action;

import com.dbn.assistant.chat.ui.ChatHistoryDialog;
import com.dbn.assistant.chat.ui.ChatHistoryForm;
import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * Action for deleting a stored chat
 **/
public class ChatHistoryDeleteAction extends AbstractChatBoxAction {
    public ChatHistoryDeleteAction() {
        super(txt("app.assistant.action.AssistantDeleteConversations"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatHistoryForm chatHistoryForm = e.getData(DataKeys.CHAT_HISTORY_FORM);
        if (chatHistoryForm == null) return;

        ChatHistoryDialog chatHistoryDialog = chatHistoryForm.getParentDialog();
        if (chatHistoryDialog != null) chatHistoryDialog.performDeleteAction();

    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatHistoryForm chatHistoryForm = e.getData(DataKeys.CHAT_HISTORY_FORM);
        if (chatHistoryForm == null) return;

        int selectedRowCount = chatHistoryForm.getSelectedRowCount();

        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_DELETE);
        presentation.setText(txt("app.assistant.action.DeleteConversations"));
        presentation.setEnabled(selectedRowCount > 0);
    }

}
