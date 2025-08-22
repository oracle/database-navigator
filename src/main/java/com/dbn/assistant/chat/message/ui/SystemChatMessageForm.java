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
import com.dbn.assistant.chat.message.action.AssistantHelpAction;
import com.dbn.common.message.MessageType;
import com.dbn.common.text.TextContent;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

public class SystemChatMessageForm extends ChatMessageForm {

    private JPanel mainPanel;
    private JPanel actionPanel;
    private JLabel titleLabel;
    private JPanel titlePanel;
    private JPanel contentPanel;
    private JPanel messagePanel;

    public SystemChatMessageForm(ChatMessagesForm parent, ChatMessage message) {
        super(parent, message);

        initTitlePanel();
        initActionToolbar();
        initMessagePanel();
    }

    private void initMessagePanel() {
        String messageContent = getMessage().getContent();
        TextContent content = TextContent.plain(messageContent);
        ChatMessageSectionTextForm messageSectionForm = new ChatMessageSectionTextForm(this, content);
        messagePanel.add(messageSectionForm.getComponent());
    }

    private void createUIComponents() {
        contentPanel = createContentPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected @Nullable JLabel getTitleLabel() {
        return titleLabel;
    }

    @Override
    protected AnAction[] createActions() {
        return new AnAction[]{new AssistantHelpAction()};
    }

    @Override
    protected JPanel getActionPanel() {
        return actionPanel;
    }

    @Override
    protected JPanel getContentPanel() {
        return contentPanel;
    }

    @Override
    protected Color getBackground() {
        MessageType messageType = getMessage().getType();
        return messageType == MessageType.ERROR ?
                Backgrounds.SYSTEM_ERROR :
                Backgrounds.SYSTEM_INFO;
    }
}
