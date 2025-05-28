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

public class ConversationHistoryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JBScrollPane tableScrollPane;
    private JBTextField filterTextField;
    private JPanel conversationActionsPanel;
    private ConversationHistoryTable conversationTable;

    public ConversationHistoryForm(@NotNull ConversationHistoryDialog parent, List<ChatConversation> conversations) {
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

        filterTextField.getEmptyText().setText("Filter");
        createActionPanel();
    }

    public int getSelectedRowCount() {
        return conversationTable.getSelectedRowCount();
    }
    private void updateDialogButtonState(ConversationHistoryDialog parent) {
        int selectedRowCount = getSelectedRowCount();
        if (selectedRowCount == 0) parent.getOKAction().setEnabled(false);
        else parent.getOKAction().setEnabled(selectedRowCount == 1);
    }

    public void setConversations(List<ChatConversation> conversations) {
        conversationTable.getModel().setConversations(conversations);
        conversationTable.adjustColumnWidths();
        updateDialogButtonState(getParentComponent());
    }

    public String[] getSelectedConversationIds() {
        int[] selectedRows = conversationTable.getSelectedRows();
        String[] ids = new String[selectedRows.length];

        for (int i = 0; i < selectedRows.length; i++) {
            int modelRow = conversationTable.convertRowIndexToModel(selectedRows[i]);
            ids[i] = conversationTable.getModel().getIdAt(modelRow);
        }

        return ids;
    }

    public String getSelectedConversationId() {
        int selectedRow = conversationTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = conversationTable.convertRowIndexToModel(selectedRow);
            return conversationTable.getModel().getIdAt(modelRow);
        }
        return null;
    }

    private void createActionPanel(){
        ActionToolbar conversationActions = Actions.createActionToolbar(conversationActionsPanel, true, "DBNavigator.ActionGroup.AssistantConversationHistory");
        setAccessibleName(conversationActions, "Conversation History");
        this.conversationActionsPanel.add(conversationActions.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.CONVERSATION_HISTORY_FORM.is(dataId)) return this;
        return null;
    }
}