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

package com.dbn.assistant.chat.ui;

import com.dbn.assistant.chat.ChatConversation;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.util.Conditional.when;

public class ConversationHistoryDialog extends DBNDialog<ConversationHistoryForm> {
    private final List<ChatConversation> conversations;
    private final Consumer<String> openAction;
    private final Consumer<List<String>> deleteAction;

    public ConversationHistoryDialog(
            Project project,
            List<ChatConversation> conversations,
            Consumer<String> openAction,
            Consumer<List<String>> deleteAction) {
        super(project, "Conversation History", true);
        this.conversations = conversations;
        this.openAction = openAction;
        this.deleteAction = deleteAction;

        getOKAction().setEnabled(false);
        setDefaultSize(600, 300);
        renameAction(getOKAction(), "Open Conversation");
        renameAction(getCancelAction(), "Close");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected ConversationHistoryForm createForm() {
        return new ConversationHistoryForm(this, conversations);
    }

    @Override
    @NotNull
    protected final Action @NotNull [] createActions() {
        return new Action[]{
                getCancelAction(),
                getOKAction()
        };
    }

    @Override
    public void doCancelAction() {
        close(0);
    }

    @Override
    protected void doOKAction() {
        String selectedId = getForm().getSelectedConversationId();

        if (selectedId != null) {
            openAction.accept(selectedId);
        }

        close(2);
    }

    /**
     * Handles the delete action by confirming with the user and then calling the delete consumer
     */
    public void performDeleteAction() {
        String[] selectedIds = getForm().getSelectedConversationIds();
        if (selectedIds.length == 0) return;

        String confirmMessage = selectedIds.length == 1
                ? "Are you sure you want to delete this conversation?"
                : "Are you sure you want to delete these " + selectedIds.length + " conversations?";

        String[] options = {"No", "Yes"};

        Messages.showQuestionDialog(
                getProject(),
                "Confirm Deletion",
                confirmMessage,
                options,
                1,
                option -> when(option == 1, () -> deleteConversations(selectedIds))
        );
    }

    private void deleteConversations(String[] selectedIds) {
        List<String> conversationIdsToDelete = Arrays.asList(selectedIds);
        conversations.removeIf(c -> conversationIdsToDelete.contains(c.getId()));
        deleteAction.accept(conversationIdsToDelete);

        getForm().setConversations(conversations);
    }

    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}