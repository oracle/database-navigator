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

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class MessageBundleForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel messagesPanel;

    public MessageBundleForm(MessageBundleDialog dialog) {
        super(dialog);

        initHeaderPanel();
        initHintPanel();
        initMessagesPanel();
    }

    private void initHeaderPanel() {
        Object contextObject = getDialog().getContextObject();
        if (contextObject == null) {
            headerPanel.setVisible(false);
        } else {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, contextObject);
            headerPanel.add(headerForm.getMainComponent());
        }
    }

    private void initHintPanel() {
        String mainMessage = getDialog().getMainMessage();
        if (Strings.isEmptyOrSpaces(mainMessage)) {
            hintPanel.setVisible(false);
        } else {
            TextContent hintText = TextContent.plain(mainMessage);
            DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
            hintPanel.add(hintForm.getMainComponent());
        }
    }

    private void initMessagesPanel() {
        verticalBoxLayout(messagesPanel);
        List<Message> messages = getMessageBundle().getMessages();
        for (Message message : messages) {
            MessageBundleItemForm messageItemForm = new MessageBundleItemForm(this, message);
            messagesPanel.add(messageItemForm.getMainComponent());
        }

    }

    public MessageBundle getMessageBundle() {
        MessageBundleDialog dialog = getDialog();
        return dialog.getMessageBundle();
    }

    @NotNull
    private MessageBundleDialog getDialog() {
        return ensureParentComponent();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
