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

import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.UserInterface;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class ChatMessagesForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel messagesPanel;
    private JScrollPane messagesScrollPanel;

    private final List<ChatMessageForm> messageForms = DisposableContainers.list(this);

    public ChatMessagesForm(@Nullable DBNComponent parent) {
        super(parent);

        verticalBoxLayout(messagesPanel);
        ClientProperty.HORIZONTAL_SCROLL_POLICY.set(messagesScrollPanel, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }


    private void removeProgressIndicator() {
        Component[] messagePanels = messagesPanel.getComponents();
        if (messagePanels.length == 0) return;

        Component panel = messagePanels[messagePanels.length - 1];
        if (panel instanceof JComponent) {
            // identify the message panels that have progress indicators and hide them
            JComponent component = (JComponent) panel;
            UserInterface.visitRecursively(component, JProgressBar.class, b -> b.setVisible(false));
        }
    }

    public void addMessages(List<ChatMessage> chatMessages) {
        dispatch(() -> {
            removeProgressIndicator();

            for (ChatMessage message : chatMessages) {
                ChatMessageForm form = ChatMessageForm.create(this, message);
                this.messageForms.add(form);
                this.messagesPanel.add(form.getComponent());
            }
            UserInterface.repaint(mainPanel);
            scrollDown();
        });
    }

    @Nullable
    public ChatMessageForm getNextMessageForm(ChatMessageForm messageForm) {
        int index = messageForms.indexOf(messageForm);
        int nextIndex = index + 1;
        if (nextIndex >= messageForms.size()) return null;

        return messageForms.get(index + 1);
    }

    public void clear() {
        List<ChatMessageForm> messageForms = new ArrayList<>(this.messageForms);
        this.messageForms.clear();
        this.messagesPanel.removeAll();
        Disposer.dispose(messageForms);
        UserInterface.repaint(mainPanel);
    }

    public void scrollDown() {
        messagesScrollPanel.validate();
        JScrollBar verticalBar = messagesScrollPanel.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());

    }
}
