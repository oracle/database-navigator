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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Set;

public class AssistantCredentialEditForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JBTextField nameTextField;
    private JBTextField userTextField;
    private JBPasswordField keyPasswordField;
    private DBNComboBox<AIProvider> providerComboBox;


    private final AssistantCredential credential;
    private final Set<String> usedNames;

    AssistantCredentialEditForm(AssistantCredentialEditDialog parent, Set<String> usedNames) {
        super(parent);
        this.credential = parent.getCredential();
        this.usedNames = usedNames;

        ComboBoxes.initComboBox(providerComboBox, getProviders());
        resetFormChanges();
    }

    private static List<AIProvider> getProviders() {
        return AIProviderData.getProviders(AssistantType.PUBLIC);
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, Strings::isNotEmpty, "Please provide a credential name");
        addTextValidation(nameTextField, this::isNotUsed, "The credential name is already in use");
        addTextValidation(keyPasswordField, Strings::isNotEmpty, "Please provide a credential key");
    }

    public void applyFormChanges() {
        credential.setName(nameTextField.getText());
        credential.setUser(userTextField.getText());
        credential.setKey(keyPasswordField.getPassword());

        AIProvider provider = ComboBoxes.getSelection(providerComboBox);
        credential.setProvider(provider == null ? null : provider.getId());
    }

    public void resetFormChanges() {
        nameTextField.setText(credential.getName());
        userTextField.setText(credential.getUser());
        keyPasswordField.setText(Chars.toString(credential.getKey()));

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, credential.getProvider());
        ComboBoxes.setSelection(providerComboBox, provider);
    }

    private boolean isNotUsed(String name) {
        return !usedNames.contains(name);
    }

    public String getCredentialName() {
        return nameTextField.getText();
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
