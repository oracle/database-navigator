/*
 * Copyright 2026 Oracle and/or its affiliates
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

import androidx.annotation.Nullable;
import com.dbn.assistant.chat.message.ChatMessageNoteSection;
import com.dbn.assistant.chat.message.action.DiscardContentAction;
import com.dbn.assistant.chat.message.action.ToggleFoldingAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNDiscardableComponent;
import com.dbn.common.ui.component.DBNFoldableComponent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Dimension;

public class ChatMessageNoteSectionForm extends ChatMessageSectionForm<ChatMessageNoteSection> implements DBNFoldableComponent, DBNDiscardableComponent {
    private JTextPane messageTextPane;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JPanel actionsPanel;
    private JPanel contentPanel;
    private JPanel messagePanel;

    public ChatMessageNoteSectionForm(DBNForm parent, ChatMessageNoteSection section) {
        super(parent, section);

        initContentPanel();
        initActionsPanel();
        initTitleLabel();
        initMessagePane();

        updateContent(getMessage().getText());
        whenSettingsChange(() -> rebuildContent());
    }

    TitledMessage getMessage() {
        return getSection().getMessage();
    }

    private void initContentPanel() {
        TitledMessage message = getMessage();
        MessageType messageType = message.getType();
        this.contentPanel.setBackground(messageType.getBannerBackgroundColor());
        this.contentPanel.setBorder(Borders.lineBorder(messageType.getBannerBorderColor(), 1));
    }

    private void initMessagePane() {
        messageTextPane.setForeground(Colors.Banner.FOREGROUND);
        messagePanel.setVisible(!getSection().isFolded());
    }

    private void initTitleLabel() {
        TitledMessage message = getMessage();
        titleLabel.setText(message.getTitle());
        //titleLabel.setIcon(message.getType().getTitleIcon());
        titleLabel.setForeground(Colors.Banner.FOREGROUND);
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new ToggleFoldingAction(),
                new DiscardContentAction());
        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        this.actionsPanel.add(component);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void setForeground(Color foreground) {
        messageTextPane.setForeground(foreground);
    }

    @Override
    protected void applyContent(TextContent content, @Nullable Language language) {
        messageTextPane.setContentType(content.getTypeId());
        messageTextPane.setText(content.getText());

        Dimension preferredSize = messageTextPane.getPreferredSize();
        //preferredSize = Dimensions.change(preferredSize, 4, 4);
        messageTextPane.setSize(preferredSize);
        messageTextPane.revalidate();
    }

    public void updateNoteContent(ChatMessageNoteSection section) {

    }

    public Object getData(@NotNull String dataId) {
        if (DataKeys.FOLDABLE_COMPONENT.is(dataId)) return this;
        if (DataKeys.DISCARDABLE_COMPONENT.is(dataId)) return this;
        return null;
    }

    @Override
    public boolean isFolded() {
        return getSection().isFolded();
    }

    @Override
    public void setFolded(boolean folded) {
        getSection().setFolded(folded);
        messagePanel.setVisible(!folded);
    }

    @Override
    public void discard() {
        AgentChatMessageForm messageForm = ensureParentFrom(AgentChatMessageForm.class);
        messageForm.discardContent(this);
    }
}
