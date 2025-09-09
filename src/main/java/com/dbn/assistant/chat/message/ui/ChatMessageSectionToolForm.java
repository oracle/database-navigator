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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessageToolSection;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolInfo.UtilityDefinition;
import com.dbn.assistant.tool.event.AssistantToolStatus;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.lang.Language;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.assistant.chat.message.ChatMessageSectionType.TOOL;
import static com.dbn.assistant.tool.AssistantToolCache.getUtilityDefinition;
import static com.intellij.icons.AllIcons.General.ExternalTools;

public class ChatMessageSectionToolForm extends ChatMessageSectionForm{
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel requestPanel;
    private JPanel actionsPanel;
    private JPanel responsePanel;
    private JPanel processIconPanel;
    private JLabel headerLabel;
    private JButton allowButton;
    private JButton denyButton;

    private final ConnectionRef connection;
    private final ChatMessageToolSection toolSection;

    ChatMessageSectionToolForm(DBNForm parent, ConnectionHandler connection, ChatMessageToolSection toolSection) {
        super(parent, TOOL);
        this.connection = ConnectionRef.of(connection);
        this.toolSection = toolSection;

        initHeaderPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void initHeaderPanel() {
        String toolName = toolSection.getToolName();
        headerLabel.setText(toolName);
        headerLabel.setIcon(ExternalTools);
        AssistantToolCache toolCache = getToolCache();
        if (toolCache == null) return;

        AssistantTool tool = toolCache.getAssistantTool(toolName);
        if (tool == null) return;

        UtilityDefinition definition = getUtilityDefinition(tool, toolName);
        if (definition == null) return;

        headerLabel.setText(definition.name());
    }

    @Nullable
    private AssistantToolCache getToolCache() {
        ChatBoxForm chatBoxForm = getChatBoxForm();
        if (chatBoxForm == null) return null;

        AssistantState assistantState = chatBoxForm.getAssistantState();
        return AssistantToolCache.get(assistantState);
    }

    @Nullable
    private ChatBoxForm getChatBoxForm() {
        return getParentFrom(ChatBoxForm.class);
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    protected void applyContent(TextContent content, @Nullable Language language) {

    }

    public void updateToolContent(ChatMessageToolSection section) {
        AssistantToolStatus toolStatus = section.getToolStatus();
        if (toolStatus != AssistantToolStatus.REQUESTED) {
            actionsPanel.setVisible(false);
        }
    }
}
