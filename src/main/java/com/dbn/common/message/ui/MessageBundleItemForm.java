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

package com.dbn.common.message.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.message.Message;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Titled;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;

public class MessageBundleItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextPane messageTextPane;
    private JPanel iconPanel;
    private JLabel iconLabel;
    private JLabel titleLabel;

    private final Message message;

    public MessageBundleItemForm(MessageBundleForm parent, Message message) {
        super(parent);
        this.message = message;

        initMessageIcon();
        initMessageTitle();
        initMessageText();
    }

    private void initMessageIcon() {
        Icon icon = getMessageIcon();
        iconLabel.setIcon(icon);
        iconLabel.setText("");
    }

    private void initMessageTitle() {
        String messageTitle = getMessageTitle();
        if (isEmptyOrSpaces(messageTitle)) {
            titleLabel.setText("");
            titleLabel.setVisible(false);
        } else {
            titleLabel.setText(messageTitle);
        }
    }

    private void initMessageText() {
        messageTextPane.setText(message.getText());
    }

    private Icon getMessageIcon() {
        switch (message.getType()) {
            case INFO: return Icons.COMMON_INFO;
            case WARNING: return Icons.COMMON_WARNING;
            case ERROR: return Icons.COMMON_ERROR;
            default: return null;
        }
    }

    @Nullable
    private String getMessageTitle() {
        if (message instanceof Titled) {
            Titled titled = (Titled) message;
            return titled.getTitle();
        }
        return null;
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
