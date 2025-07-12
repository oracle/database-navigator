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

import com.dbn.assistant.chat.Chat;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.util.Conditional.when;

public class ChatHistoryDialog extends DBNDialog<ChatHistoryForm> {
    private final List<Chat> chats;
    private final Consumer<String> openAction;
    private final Consumer<List<String>> deleteAction;

    public ChatHistoryDialog(
            Project project,
            List<Chat> chats,
            Consumer<String> openAction,
            Consumer<List<String>> deleteAction) {
        super(project, "Chat History", true);
        this.chats = chats;
        this.openAction = openAction;
        this.deleteAction = deleteAction;

        getOKAction().setEnabled(false);
        setDefaultSize(600, 300);
        renameAction(getOKAction(), "Open Chat");
        renameAction(getCancelAction(), "Close");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected ChatHistoryForm createForm() {
        return new ChatHistoryForm(this, chats);
    }

    @Override
    @NotNull
    protected final Action [] createActions() {
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
        String selectedId = getForm().getSelectedChatId();

        close(2);

        if (selectedId != null) {
            openAction.accept(selectedId);
        }
    }

    /**
     * Handles the delete action by confirming with the user and then calling the delete consumer
     */
    public void performDeleteAction() {
        String[] selectedIds = getForm().getSelectedChatIds();
        if (selectedIds.length == 0) return;

        String confirmMessage = selectedIds.length == 1 ?
                "Are you sure you want to delete this chat?" :
                "Are you sure you want to delete the " + selectedIds.length + " selected chats?";

        Messages.showQuestionDialog(
                getProject(),
                "Delete Chats",
                confirmMessage,
                Messages.OPTIONS_YES_NO,
                0,
                option -> when(option == 0, () -> deleteChats(selectedIds))
        );
    }

    private void deleteChats(String[] selectedIds) {
        List<String> chatIdsToDelete = Arrays.asList(selectedIds);
        chats.removeIf(c -> chatIdsToDelete.contains(c.getId()));
        deleteAction.accept(chatIdsToDelete);

        getForm().setChats(chats);
    }

    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}