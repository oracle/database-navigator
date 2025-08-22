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
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.Layouts;
import com.dbn.connection.ConnectionHandler;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageParser.convertMarkdownToHtml;

/**
 * Message for implementation for AI agent responses.
 * Features code viewers for the code-qualified sections of the message
 *
 * @author Dan Cioca (Oracle)
 */
public class AgentChatMessageForm extends ChatMessageForm {

    private JPanel mainPanel;
    private JPanel sectionsPanel;
    private JLabel titleLabel;
    private JPanel actionPanel;
    private JPanel contentPanel;

    private boolean hasCodeContents = false;
    private List<ChatMessageSectionForm> sectionForms = DisposableContainers.list(this);

    public AgentChatMessageForm(ChatMessagesForm parent, ChatMessage message) {
        super(parent, message);

        initTitlePanel();
        initMessagePanels();
        initActionToolbar();
    }

    private void createUIComponents() {
        contentPanel = createContentPanel();
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

    @Override
    protected JPanel getContentPanel() {
        return contentPanel;
    }

    private void initMessagePanels() {
        ChatMessage message = getMessage();
        Layouts.verticalBoxLayout(sectionsPanel);
        for (ChatMessageSection section : message.getSections()) {
            createSectionForm(section);
        }
    }

    private void createSectionForm(ChatMessageSection section) {
        if (section.getLanguage() == null)
            createTextSectionForm(section); else
            createCodeSectionForm(section);
    }

    private void updateSectionForm(ChatMessageSection section, ChatMessageSectionForm sectionForm) {
        if (section.getLanguage() == null)
            updateTextSectionForm(sectionForm, section); else
            updateCodeSectionForm(sectionForm, section);
    }

    protected void createTextSectionForm(ChatMessageSection section) {
        TextContent content = convertMarkdownToHtml(section.getContent());

        ChatMessageSectionTextForm messageSectionForm = new ChatMessageSectionTextForm(this, content);
        sectionForms.add(messageSectionForm);
        sectionsPanel.add(messageSectionForm.getComponent());

        whenSettingsChange(() -> {
            content.rebuild();
            messageSectionForm.setContent(content);
        });
    }

    private void createCodeSectionForm(ChatMessageSection section) {
        ChatMessagesForm parent = ensureParentComponent();
        ChatBoxForm chatBoxForm = parent.ensureParentComponent();
        ConnectionHandler connection = chatBoxForm.getConnection();

        ChatMessageSectionCodeForm messageSectionForm = ChatMessageSectionCodeForm.create(parent, connection, section);
        if (messageSectionForm == null) {
            // fallback to regular text pane if code panel creation was unsuccessful
            createTextSectionForm(section);
            return;
        }

        sectionForms.add(messageSectionForm);
        sectionsPanel.add(messageSectionForm.getComponent());
        hasCodeContents = true; // mark as having code contents if successfully created one
    }

    protected void updateTextSectionForm(ChatMessageSectionForm form, ChatMessageSection section) {
        TextContent content = convertMarkdownToHtml(section.getContent());
        form.updateContent(content);
    }

    protected void updateCodeSectionForm(ChatMessageSectionForm form, ChatMessageSection section) {
        TextContent content = TextContent.plain(section.getContent());
        form.updateContent(content);
    }

    @Override
    public void refreshContent() {
        ChatMessage message = getMessage();
        List<ChatMessageSection> sections = message.getSections();
        for (int i = 0; i < sections.size(); i++) {
            ChatMessageSection section = sections.get(i);
            if (i < sectionForms.size()) {
                ChatMessageSectionForm sectionForm = sectionForms.get(i);
                updateSectionForm(section, sectionForm);
            } else {
                createSectionForm(section);
            }
        }
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
