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
import com.dbn.common.action.DataKeys;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ChatHistoryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JBScrollPane tableScrollPane;
    private JBTextField filterTextField;
    private JPanel chatActionsPanel;
    private final ChatHistoryTable chatHistoryTable;

    public ChatHistoryForm(@NotNull ChatHistoryDialog parent, List<Chat> chats) {
        super(parent);

        chatHistoryTable = new ChatHistoryTable(this, chats);
        tableScrollPane.setViewportView(chatHistoryTable);

        chatHistoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDialogButtonState(parent);
            }
        });

        chatHistoryTable.setDoubleClickAction(() -> {
            if (parent.getOKAction().isEnabled()) {
                parent.doOKAction();
            }
        });

        TextFields.onTextChange(filterTextField, e ->
                chatHistoryTable.getModel().filter(filterTextField.getText())
        );

        chatHistoryTable.adjustColumnWidths();
        updateDialogButtonState(parent);

        filterTextField.getEmptyText().setText("Filter");
        createActionPanel();
    }

    public int getSelectedRowCount() {
        return chatHistoryTable.getSelectedRowCount();
    }
    private void updateDialogButtonState(ChatHistoryDialog parent) {
        int selectedRowCount = getSelectedRowCount();
        if (selectedRowCount == 0) parent.getOKAction().setEnabled(false);
        else parent.getOKAction().setEnabled(selectedRowCount == 1);
    }

    public void setChats(List<Chat> chats) {
        chatHistoryTable.getModel().setChats(chats);
        chatHistoryTable.adjustColumnWidths();
        updateDialogButtonState(getParentComponent());
    }

    public String[] getSelectedChatIds() {
        int[] selectedRows = chatHistoryTable.getSelectedRows();
        String[] ids = new String[selectedRows.length];

        for (int i = 0; i < selectedRows.length; i++) {
            int modelRow = chatHistoryTable.convertRowIndexToModel(selectedRows[i]);
            ids[i] = chatHistoryTable.getModel().getIdAt(modelRow);
        }

        return ids;
    }

    public String getSelectedChatId() {
        int selectedRow = chatHistoryTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = chatHistoryTable.convertRowIndexToModel(selectedRow);
            return chatHistoryTable.getModel().getIdAt(modelRow);
        }
        return null;
    }

    private void createActionPanel(){
        ActionToolbar chatActions = Actions.createActionToolbar(chatActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatHistory");
        setAccessibleName(chatActions, "Chat History");
        this.chatActionsPanel.add(chatActions.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.CHAT_HISTORY_FORM.is(dataId)) return this;
        return null;
    }
}