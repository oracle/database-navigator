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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SaveOrDiscardConversationForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JTextField conversationTitleTextField;

    SaveOrDiscardConversationForm(SaveOrDiscardConversationDialog parent, String changedField) {
        super(parent);
        JLabel warningLabel = new JLabel(
                "<html>By changing the <b>" + changedField + "</b>, your current conversation will be interrupted. " +
                        "Do you want to save this conversation?</html>"
        );
        warningLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        headerPanel.add(warningLabel);
    }

    @Override
    protected void initValidation() {
        addTextValidation(conversationTitleTextField, Strings::isNotEmpty, "");
        addTextValidation(conversationTitleTextField, this::isNotUsed, "Conversation name already in use");
    }

    private boolean isNotUsed(String name) {
        return true;
    }

    public String getConversationTitle() {
        return conversationTitleTextField.getText();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return conversationTitleTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
