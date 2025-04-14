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

import com.dbn.assistant.chat.ui.ConversationHistoryDialog;
import com.dbn.assistant.chat.ui.ConversationHistoryForm;
import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * Action for deleting a stored conversation
 **/
public class ConversationDeleteAction extends AbstractChatBoxAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConversationHistoryForm conversationHistoryForm = e.getData(DataKeys.CONVERSATION_HISTORY);
        if (conversationHistoryForm == null) return;
        ConversationHistoryDialog conversationHistoryDialog = conversationHistoryForm.getParentDialog();
        if (conversationHistoryDialog != null) conversationHistoryDialog.performDeleteAction();

    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        ConversationHistoryForm conversationHistoryForm = e.getData(DataKeys.CONVERSATION_HISTORY);
        if (conversationHistoryForm == null) return;
        int selectedRowCount = conversationHistoryForm.getSelectedRowCount();

        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_DELETE);
        presentation.setText(txt("app.assistant.action.ClearConversation"));
        presentation.setEnabled(selectedRowCount > 0);
    }

}
