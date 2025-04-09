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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class ConversationHistoryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JBScrollPane tableScrollPane;
    private JTextField filterTextField;
    private ConversationHistoryTable conversationTable;

    public ConversationHistoryForm(@NotNull ConversationHistoryDialog parent, List<PersistentChatConversation> conversations) {
        super(parent);

        conversationTable = new ConversationHistoryTable(this, conversations);
        tableScrollPane.setViewportView(conversationTable);

        conversationTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDialogButtonState(parent);
            }
        });

        conversationTable.setDoubleClickAction(() -> {
            if (parent.getOKAction().isEnabled()) {
                parent.doOKAction();
            }
        });

        TextFields.onTextChange(filterTextField, e ->
                conversationTable.getModel().filter(filterTextField.getText())
        );

        conversationTable.adjustColumnWidths();
        updateDialogButtonState(parent);
    }

    private void updateDialogButtonState(ConversationHistoryDialog parent) {
        int selectedRowCount = conversationTable.getSelectedRowCount();

        if (selectedRowCount == 0) {
            parent.getOKAction().setEnabled(false);
            parent.getDeleteButtonAction().setEnabled(false);
        } else if (selectedRowCount == 1) {
            parent.getOKAction().setEnabled(true);
            parent.getDeleteButtonAction().setEnabled(true);
        } else {
            parent.getOKAction().setEnabled(false);
            parent.getDeleteButtonAction().setEnabled(true);
        }
    }

    public void setConversations(List<PersistentChatConversation> conversations) {
        conversationTable.getModel().setConversations(conversations);
        conversationTable.adjustColumnWidths();
        updateDialogButtonState((ConversationHistoryDialog) getParentComponent());
    }

    public Object[] getSelectedConversationIds() {
        int[] selectedRows = conversationTable.getSelectedRows();
        Object[] ids = new Object[selectedRows.length];

        for (int i = 0; i < selectedRows.length; i++) {
            int modelRow = conversationTable.convertRowIndexToModel(selectedRows[i]);
            ids[i] = conversationTable.getModel().getIdAt(modelRow);
        }

        return ids;
    }

    public Object getSelectedConversationId() {
        int selectedRow = conversationTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = conversationTable.convertRowIndexToModel(selectedRow);
            return conversationTable.getModel().getIdAt(modelRow);
        }
        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}