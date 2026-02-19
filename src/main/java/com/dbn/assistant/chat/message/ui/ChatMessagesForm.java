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
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.tool.event.AssistantToolEvent;
import com.dbn.assistant.tool.event.AssistantToolListener;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.Components;
import com.dbn.common.ui.util.ScrollPanes;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Alarms;
import com.dbn.common.util.Lists;
import com.intellij.util.Alarm;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class ChatMessagesForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel messagesPanel;
    private JScrollPane messagesScrollPanel;
    private final Alarm scrollAlarm;

    @Getter
    private final List<ChatMessageForm> messageForms = DisposableContainers.list(this);

    public ChatMessagesForm(@Nullable DBNComponent parent) {
        super(parent);

        scrollAlarm = Alarms.createAlarm(this);
        ClientProperty.HORIZONTAL_SCROLL_POLICY.set(messagesScrollPanel, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        verticalBoxLayout(messagesPanel);
        Components.onComponentResized(messagesPanel, e -> messagesPanel.revalidate());

        ProjectEvents.subscribe(AssistantToolListener.TOPIC, createToolListener());
    }

    private AssistantToolListener createToolListener() {
        return event -> {
            if (matchesChat(event)) {
                scrollDown();
            }
        };
    }

    private boolean matchesChat(AssistantToolEvent event) {
        AssistantToolRequest request = event.getRequest();
        ChatBoxForm chatBoxForm = getParentFrom(ChatBoxForm.class);
        if (chatBoxForm == null) return false;

        String requestChatId = request.getChatId();
        Object currentChatId = chatBoxForm.getCurrentChatId();
        return Objects.equals(currentChatId, requestChatId);

    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }


    private void removeProgressIndicator() {
        Component[] messagePanels = messagesPanel.getComponents();
        if (messagePanels.length == 0) return;

        Component panel = messagePanels[messagePanels.length - 1];
        if (panel instanceof JComponent component) {
            // identify the message panels that have progress indicators and hide them
            UserInterface.visitRecursively(component, JProgressBar.class, b -> b.setVisible(false));
        }
    }

    public void addMessages(List<ChatMessage> chatMessages) {
        Dispatch.run(mainPanel, () -> {
            removeProgressIndicator();

            for (ChatMessage message : chatMessages) {
                ChatMessageForm form = ChatMessageForm.create(this, message);
                this.messageForms.add(form);
                this.messagesPanel.add(form.getComponent());
            }
            this.mainPanel.revalidate();
            scrollDown();
        });
    }

    public void hideProcessingIndicators() {
        messageForms.forEach(f -> f.hideProcessingIndicators());
    }

    public void refreshMessage(ChatMessage message) {
        refreshContent(message, f -> f.refreshMessageContent());
    }

    public void refreshTools(ChatMessage message) {
        refreshContent(message, f -> f.refreshToolContent());
    }

    private void refreshContent(ChatMessage message, Consumer<ChatMessageForm> action) {
        Dispatch.execute(mainPanel, () -> {
            ChatMessageForm messageForm = getMessageForm(message.getId());
            if (messageForm == null) return;

            action.accept(messageForm);
            scrollDown();
        });
    }

    private @Nullable ChatMessageForm getMessageForm(String messageId) {
        return Lists.first(messageForms, form -> form.getMessage().getId().equals(messageId));
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
        this.messagesPanel.revalidate();
        Disposer.dispose(messageForms);
    }

    public void scrollDown() {
        scrollAlarm.cancelAllRequests();
        scrollAlarm.addRequest(() -> ScrollPanes.scrollDown(messagesScrollPanel, false), 10);
    }

    public void expandAllMessages() {
        messageForms.forEach(m -> m.changeContentFolding(false));
    }

    public void collapseAllMessages() {
        messageForms.forEach(m -> m.changeContentFolding(true));
    }
}
