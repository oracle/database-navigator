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

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.panel.DBNPanelImpl;
import com.dbn.common.ui.util.Fonts;
import com.intellij.openapi.Disposable;
import com.intellij.ui.JBColor;

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

    public void update(int status) {
        Dispatch.run(true, () -> {
            chatStatusLabel.setForeground(status == 0 ? Colors.CONVERSATIONAL : status == 1 ? Colors.NON_CONVERSATIONAL : Colors.CONVERSATION_DISCONTINUED);
            chatStatusLabel.setText(status == 0 ? "Interactive" : status == 1 ? "Non-Interactive" : "Discontinued");
            chatStatusLabel.setToolTipText("Status of current chat");
        });
    }

}

