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
package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import lombok.Getter;

import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;

public class ChatBoxStatusLabel extends DBNPanelImpl implements Disposable {
    private interface Colors {
        Color CONVERSATIONAL = new JBColor(new Color(0x55A76A), new Color(0x57965C));  // Green
        Color NON_CONVERSATIONAL = new JBColor(new Color(0xFFAF0F), new Color(0xF2C55C));  // Yellow
        Color CONVERSATION_DISCONTINUED = new JBColor(new Color(0x818594), new Color(0x9DA0A8));  // Grey
    }

    private final WeakRef<ChatBoxForm> chatBox;
    private final JLabel chatStatusLabel;

    public ChatBoxStatusLabel(ChatBoxForm chatBox) {
        super(chatBox);
        this.chatBox = WeakRef.of(chatBox);

        setLayout(new BorderLayout());

        chatStatusLabel = new JLabel();
        chatStatusLabel.setFont(Fonts.regularBold());
        add(chatStatusLabel, BorderLayout.WEST);

        add(new JLabel(" "), BorderLayout.CENTER);

        Project project = chatBox.ensureProject();
        ProjectEvents.subscribe(project, this, AssistantStateListener.TOPIC, createStateListener());
        ProjectEvents.subscribe(project, this, ConnectionStatusListener.TOPIC, createConnectionListener());

    }

    private ConnectionStatusListener createConnectionListener() {
        return (connectionId, sessionId) -> {
            if (connectionId != getConnectionId()) return;
            if (sessionId != SessionId.ASSISTANT) return;

            refreshComponentState();
        };
    }

    private AssistantStateListener createStateListener() {
        return (project, connectionId) -> {
            if (connectionId != getConnectionId()) return;

            refreshComponentState();
        };
    }

    private ConnectionId getConnectionId() {
        return getChatBox().getConnection().getConnectionId();
    }

    private ChatBoxForm getChatBox() {
        return chatBox.ensure();
    }

    private void refreshComponentState() {
        Status status = evaluateStatus();
        Dispatch.run(chatStatusLabel, () -> updateLabel(status));
    }

    private Status evaluateStatus() {
        AssistantState assistantState = getChatBox().getAssistantState();
        if(!assistantState.isAvailable()) return Status.UNAVAILABLE;
        if(!assistantState.isCurrentContextValid()) return Status.UNAVAILABLE;

        if (!assistantState.isCurrentChatInteractive()) return Status.NON_INTERACTIVE;
        if (!assistantState.isCurrentChatActive()) return Status.DISCONTINUED;

        return Status.INTERACTIVE;
    }

    private void updateLabel(Status status) {
        chatStatusLabel.setForeground(status.getColor());
        chatStatusLabel.setText(status.getText());
        chatStatusLabel.setToolTipText(status.getDescription());
    }

    @Getter
    private enum Status {
        UNAVAILABLE ("", "", null),
        INTERACTIVE("Interactive", "<html>The selected profile is conversational.<br>It considers up to ten of your previous prompts in the response</html>", Colors.CONVERSATIONAL),
        NON_INTERACTIVE("Non-Interactive", "<html>The selected profile is non-conversational.<br>Every prompt is treated as an isolated question, with no consideration on any of the previous prompts</html>", Colors.NON_CONVERSATIONAL),
        DISCONTINUED("Discontinued", "<html>This conversation is discontinued can no longer be prompted against.</html>", Colors.CONVERSATION_DISCONTINUED),;

        private final Color color;
        private final String text;
        private final String description;

        Status(String text, String description, Color color) {
            this.color = color;
            this.text = text;
            this.description = description;
        }
    }

}

