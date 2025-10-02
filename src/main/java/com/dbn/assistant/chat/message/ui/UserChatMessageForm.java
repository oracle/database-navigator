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
import com.dbn.assistant.chat.message.action.AskAgainAction;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.assistant.chat.message.action.ToggleFoldingAction;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;

public class UserChatMessageForm extends ChatMessageForm {
    private JPanel mainPanel;
    private JProgressBar progressBar;
    private JPanel actionPanel;
    private JPanel messagePanel;
    private JPanel contentPanel;
    private JPanel foldingActionPanel;

    public UserChatMessageForm(ChatMessagesForm parent, ChatMessage message) {
        super(parent, message);

        initFoldingActionToolbar();
        initActionToolbar();
        initProgressBar();
        initMessagePanel();
    }

    private void initFoldingActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(foldingActionPanel, true, new ToggleFoldingAction());
        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(Borders.EMPTY_BORDER);
        foldingActionPanel.add(component);
        foldingActionPanel.setBorder(JBUI.Borders.empty(4, 4, 4, 0));

    }

    private void initProgressBar() {
        ChatMessage message = getMessage();
        progressBar.setVisible(message.isProgress());
        progressBar.setIndeterminate(true);
        progressBar.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
    }

    private void initMessagePanel() {
        String content = getMessage().getContent();
        ChatMessageTextSectionForm messageSectionForm = new ChatMessageTextSectionForm(this, content);

        messagePanel.add(messageSectionForm.getComponent());
    }

    @Override
    protected void initContentFolding(JPanel contentPanel) {
        // do not fold user messages
    }

    @Override
    protected void changeContentFolding(boolean folded) {
        ChatMessageForm nextMessageForm = getNextMessageForm();
        if (nextMessageForm == null) return;
        if (nextMessageForm instanceof UserChatMessageForm) return; // only fold agent or system messages

        nextMessageForm.changeContentFolding(folded);
    }

    private ChatMessageForm getNextMessageForm() {
        ChatMessagesForm messagesForm = getParentComponent();
        if (messagesForm == null) return null;

        return messagesForm.getNextMessageForm(this);
    }

    @Override
    protected AnAction[] createActions() {
        return new AnAction[]{
                new AskAgainAction(),
                new CopyContentAction(() -> getMessage().getContent())};
    }

    private void createUIComponents() {
        contentPanel = createContentPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
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
        return Backgrounds.USER_PROMPT;
    }

    @Override
    public void hideProcessingIndicators() {
        progressBar.setVisible(false);
    }
}
