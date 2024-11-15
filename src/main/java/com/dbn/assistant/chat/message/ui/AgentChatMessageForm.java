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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.ChatMessageSection;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.dispose.Disposer;
import com.dbn.connection.ConnectionHandler;
import com.intellij.util.ui.JBUI;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Message for implementation for AI agent responses.
 * Features code viewers for the code-qualified sections of the message
 *
 * @author Dan Cioca (Oracle)
 */
public class AgentChatMessageForm extends ChatMessageForm {

    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel titleLabel;
    private JPanel actionPanel;

    private boolean hasCodeContents = false;

    public AgentChatMessageForm(ChatBoxForm parent, ChatMessage message) {
        super(parent, message);

        initTitlePanel();
        initMessagePanels();
        initActionToolbar();
    }

    private void createUIComponents() {
        mainPanel = createMainPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected JLabel getTitleLabel() {
        return titleLabel;
    }

    @Override
    protected JPanel getActionPanel() {
        return actionPanel;
    }

    private void initMessagePanels() {
        ChatMessage message = getMessage();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        for (ChatMessageSection section : message.getSections()) {
            if (section.getLanguage() == null)
                createTextPane(section); else
                createCodePane(section);
        }
    }

    protected void createTextPane(ChatMessageSection section) {
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setEditable(false);
        textPane.setMargin(JBUI.insets(8));
        textPane.setText(section.getContent());
        contentPanel.add(textPane);
    }

    private void createCodePane(ChatMessageSection section) {
        ChatBoxForm parent = ensureParentComponent();
        ConnectionHandler connection = parent.getConnection();

        ChatMessageCodeViewer codePanel = ChatMessageCodeViewer.create(connection, section);
        if (codePanel == null) {
            // fallback to regular text pane if code panel creation was unsuccessful
            createTextPane(section);
            return;
        }
        Disposer.register(this, codePanel);

        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBackground(codePanel.getViewer().getBackgroundColor());

        contentPanel.add(actionsPanel);
        contentPanel.add(codePanel);
        hasCodeContents = true; // mark as having code contents if successfully created one
    }

    @Override
    protected void initActionToolbar() {
        if (hasCodeContents) {
            actionPanel.setVisible(false);
        } else {
            super.initActionToolbar();
        }
    }

    @Override
    protected Color getBackground() {
        return Backgrounds.AGENT_RESPONSE;
    }
}
