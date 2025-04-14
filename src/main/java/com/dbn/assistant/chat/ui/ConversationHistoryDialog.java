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

import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConversationHistoryDialog extends DBNDialog<ConversationHistoryForm> {
    private final List<PersistentChatConversation> conversations;
    private final Consumer<PersistentChatConversation> openAction;
    private final Consumer<List<PersistentChatConversation>> deleteAction;

    public ConversationHistoryDialog(
            Project project,
            List<PersistentChatConversation> conversations,
            Consumer<PersistentChatConversation> openAction,
            Consumer<List<PersistentChatConversation>> deleteAction) {
        super(project, "Conversation History", true);
        this.conversations = conversations;
        this.openAction = openAction;
        this.deleteAction = deleteAction;

        getOKAction().setEnabled(false);
        setDefaultSize(600, 300);
        renameAction(getOKAction(), "Open Conversation");
        setModal(true);
        setAutoSize(true);
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
        Object selectedId = getForm().getSelectedConversationId();

        if (selectedId != null) {
            PersistentChatConversation selectedConversation = conversations.stream()
                    .filter(conversation -> conversation.getId().equals(selectedId))
                    .findFirst()
                    .orElse(null);

            if (selectedConversation != null) {
                openAction.accept(selectedConversation);
            }
        }

        close(2);
    }

    /**
     * Handles the delete action by confirming with the user and then calling the delete consumer
     */
    public void performDeleteAction() {
        Object[] selectedIds = getForm().getSelectedConversationIds();

        if (selectedIds != null && selectedIds.length > 0) {
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
                    option -> {
                        if (option == 1) {
                            List<Object> idList = Arrays.asList(selectedIds);
                            Predicate<PersistentChatConversation> toDelete =
                                    conversation -> idList.contains(conversation.getId());

                            List<PersistentChatConversation> conversationsToDelete = conversations.stream()
                                    .filter(toDelete)
                                    .collect(Collectors.toList());

                            if (!conversationsToDelete.isEmpty()) {
                                deleteAction.accept(conversationsToDelete);

                                List<PersistentChatConversation> updatedList = new ArrayList<>(conversations);
                                updatedList.removeAll(conversationsToDelete);

                                getForm().setConversations(updatedList);
                            }
                        }
                    }
            );
        }
    }

    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}