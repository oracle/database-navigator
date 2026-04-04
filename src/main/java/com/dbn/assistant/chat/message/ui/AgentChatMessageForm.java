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
import com.dbn.assistant.chat.message.ChatMessageNoteSection;
import com.dbn.assistant.chat.message.ChatMessageSectionType;
import com.dbn.assistant.chat.message.ChatMessageTextSection;
import com.dbn.assistant.chat.message.ChatMessageToolSection;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.Layouts;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.actionSystem.AnAction;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageParser.convertMarkdownToHtml;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.CODE;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.NOTE;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.TEXT;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.TOOL;
import static com.dbn.common.util.Commons.array;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Unsafe.cast;

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

    private final List<ChatMessageSectionForm> sectionForms = DisposableContainers.list(this);

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
        for (ChatMessageTextSection section : message.getSections()) {
            int offset = section.getContentStartOffset();
            createToolSectionForms(offset);
            createNoteSectionForms(offset);
            createSectionForm(section);
        }

        int offset = message.getContent().length();
        createToolSectionForms(offset);
        createNoteSectionForms(offset);
    }

    private void createSectionForm(ChatMessageTextSection section) {
        if (section.getLanguage() == null)
            createTextSectionForm(section); else
            createCodeSectionForm(section);
    }

    protected void createTextSectionForm(ChatMessageTextSection section) {
        ChatMessageTextSectionForm messageSectionForm = new ChatMessageTextSectionForm(this,
                section,
                c -> convertMarkdownToHtml(c));

        sectionForms.add(messageSectionForm);
        sectionsPanel.add(messageSectionForm.getComponent());
    }

    private void createCodeSectionForm(ChatMessageTextSection section) {
        ChatMessagesForm parent = ensureParentComponent();
        ChatBoxForm chatBoxForm = parent.ensureParentComponent();
        ConnectionHandler connection = chatBoxForm.getConnection();

        ChatMessageCodeSectionForm messageSectionForm = ChatMessageCodeSectionForm.create(parent, connection, section);
        if (messageSectionForm == null) {
            // fallback to regular text pane if code panel creation was unsuccessful
            createTextSectionForm(section);
            return;
        }

        sectionForms.add(messageSectionForm);
        sectionsPanel.add(messageSectionForm.getComponent());
    }

    private boolean hasCodeSections() {
        return sectionForms.stream().anyMatch(f -> f instanceof ChatMessageCodeSectionForm);
    }

    private boolean hasToolSections() {
        return sectionForms.stream().anyMatch(f -> f instanceof ChatMessageToolSectionForm);
    }

    private void createToolSectionForms(int offset) {
        List<ChatMessageToolSection> sections = getMessage().getToolSections();
        for (ChatMessageToolSection section : sections) {
            if (section.getOffset() == offset) {
                createToolSectionForm(section);
            }
        }
    }

    private void createNoteSectionForms(int offset) {
        List<ChatMessageNoteSection> sections = getMessage().getNoteSections();
        for (ChatMessageNoteSection section : sections) {
            if (section.getOffset() == offset) {
                createNoteSectionForm(section);
            }
        }
    }

    private void createToolSectionForm(ChatMessageToolSection toolSection) {
        ChatBoxForm chatBoxForm = ensureParentFrom(ChatBoxForm.class);
        ConnectionHandler connection = chatBoxForm.getConnection();

        ChatMessageToolSectionForm toolSectionForm = new ChatMessageToolSectionForm(this, connection, toolSection);
        sectionForms.add(toolSectionForm);
        sectionsPanel.add(toolSectionForm.getComponent());
    }

    private void createNoteSectionForm(ChatMessageNoteSection noteSection) {
        ChatMessageNoteSectionForm noteSectionForm = new ChatMessageNoteSectionForm(this, noteSection);
        sectionForms.add(noteSectionForm);
        sectionsPanel.add(noteSectionForm.getComponent());
    }

    protected void discardContent(ChatMessageSectionForm sectionForm) {
        ChatMessageSectionType sectionType = sectionForm.getSectionType();
        sectionsPanel.remove(sectionForm.getComponent());
        sectionForms.remove(sectionForm);
        Disposer.dispose(sectionForm);

        if (sectionType == NOTE) {
            getMessage().getNoteSections().remove(sectionForm.getSection());
        }

    }

    @Override
    public void refreshMessageContent() {
        ChatMessage message = getMessage();
        List<ChatMessageTextSection> sections = new ArrayList<>(message.getSections());
        List<ChatMessageSectionForm> sectionForms = getSectionForms(CODE, TEXT);
        for (int i = 0; i < sections.size(); i++) {
            ChatMessageTextSection section = sections.get(i);
            if (i < sectionForms.size()) {
                ChatMessageSectionForm sectionForm = sectionForms.get(i);
                sectionForm.updateContent(section);
            } else {
                createSectionForm(section);
            }
        }
    }

    @Override
    public void refreshToolContent() {
        ChatMessage message = getMessage();
        List<ChatMessageToolSection> sections = new ArrayList<>(message.getToolSections());
        List<ChatMessageToolSectionForm> sectionForms = getSectionForms(TOOL);
        for (int i = 0; i < sections.size(); i++) {
            ChatMessageToolSection section = sections.get(i);
            if (i < sectionForms.size()) {
                ChatMessageToolSectionForm sectionForm = sectionForms.get(i);
                sectionForm.updateToolContent(section);
            } else {
                createToolSectionForm(section);
            }
        }
    }

    @Override
    public void refreshNoteContent() {
        ChatMessage message = getMessage();
        List<ChatMessageNoteSection> sections = new ArrayList<>(message.getNoteSections());
        List<ChatMessageNoteSectionForm> sectionForms = getSectionForms(NOTE);
        for (int i = 0; i < sections.size(); i++) {
            ChatMessageNoteSection section = sections.get(i);
            if (i < sectionForms.size()) {
                ChatMessageNoteSectionForm sectionForm = sectionForms.get(i);
                sectionForm.updateNoteContent(section);
            } else {
                createNoteSectionForm(section);
            }
        }
    }

    @Override
    protected AnAction[] createActions() {
        return array(new CopyContentAction(
                () -> getMessage().getContent(),
                () -> !hasCodeSections() && !hasToolSections()));
    }

    private <T extends ChatMessageSectionForm> List<T> getSectionForms(ChatMessageSectionType ... types) {
        return cast(filter(this.sectionForms, f -> Commons.isOneOf(f.getSectionType(), types)));
    }

    @Override
    public void hideProcessingIndicators() {
        sectionForms.forEach(f -> f.hideProcessingIndicator());
    }

    @Override
    protected Color getForeground() {
        return Colors.getLabelForeground();
    }

    @Override
    protected Color getBackground() {
        return Backgrounds.AGENT_RESPONSE;
    }
}
