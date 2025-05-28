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
package com.dbn.assistant.chat.ui;

import com.dbn.assistant.chat.ChatConversation;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.util.Fonts;
import com.intellij.openapi.Disposable;
import com.intellij.ui.JBColor;
import lombok.Getter;

import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;

public class ChatStatusLabel extends DBNPanelImpl implements Disposable {
    private interface Colors {
        Color CONVERSATIONAL = new JBColor(new Color(0x55A76A), new Color(0x55A76A));  // Green
        Color NON_CONVERSATIONAL = new JBColor(new Color(0xFFAE10), new Color(0xFFAE10));  // Yellow
        Color CONVERSATION_DISCONTINUED = new JBColor(new Color(0x808080), new Color(0x808080));  // Grey
    }

    private final JLabel chatStatusLabel;

    public ChatStatusLabel() {
        setLayout(new BorderLayout());

        chatStatusLabel = new JLabel();
        chatStatusLabel.setFont(Fonts.regularBold());
        add(chatStatusLabel, BorderLayout.WEST);

        add(new JLabel(" "), BorderLayout.CENTER);

    }

    private Status evaluateStatus(ChatConversation conversation) {
        if (!conversation.isActive()) return Status.INACTIVE;
        if (!conversation.isInteractive()) return Status.NON_INTERACTIVE;

        return Status.INTERACTIVE;
    }

    public void update(ChatConversation conversation) {
        Status status = evaluateStatus(conversation);

        Dispatch.run(chatStatusLabel, () -> {
            chatStatusLabel.setForeground(status.getColor());
            chatStatusLabel.setText(status.getText());
            chatStatusLabel.setToolTipText(status.getDescription());
        });
    }

    @Getter
    private enum Status {
        UNAVAILABLE ("", "", null),
        INTERACTIVE("Interactive", "<html>The selected profile is conversational.<br>It considers up to ten of your previous prompts in the response</html>", Colors.CONVERSATIONAL),
        NON_INTERACTIVE("Non-Interactive", "<html>The selected profile is non-conversational.<br>Every prompt is treated as an isolated question, with no consideration on any of the previous prompts</html>", Colors.NON_CONVERSATIONAL),
        INACTIVE("Discontinued", "<html>This conversation is discontinued can no longer be prompted against.</html>", Colors.CONVERSATION_DISCONTINUED),;

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

