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

import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Set;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.nls.NlsResources.txt;

public class ChatSaveForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JBTextField nameTextField;
    private final Set<String> usedNames;

    ChatSaveForm(ChatSaveDialog parent, ChatInterruptionReason changedField, Set<String> usedNames) {
        super(parent);
        JLabel warningLabel = new JLabel(changedField.getConfirmationMessage());
        warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        this.headerPanel.add(warningLabel);
        this.usedNames = usedNames;
        this.nameTextField.getEmptyText().setText("Chat name");

    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, Strings::isNotEmpty, txt("msg.assistant.error.ChatNameRequired"));
        addTextValidation(nameTextField, this::isNotUsed, txt("msg.assistant.error.ChatNameAlreadyInUse"));
    }

    private boolean isNotUsed(String name) {
        return !usedNames.contains(name);
    }

    public String getChatName() {
        return getText(nameTextField);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
