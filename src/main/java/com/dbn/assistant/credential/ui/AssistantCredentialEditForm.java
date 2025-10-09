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
import com.dbn.assistant.provider.AIAuthentication;
import com.dbn.assistant.provider.AIAuthentication.Field;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.provider.ProviderUrlType;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.dbn.oci.ui.OciConfigForm;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.dbn.assistant.provider.AIAuthentication.Field.API_KEY;
import static com.dbn.assistant.provider.AIAuthentication.Field.PASSWORD;
import static com.dbn.assistant.provider.AIAuthentication.Field.USER;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;
import static com.dbn.common.util.Strings.isNotEmpty;

public class AssistantCredentialEditForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel ociConfigPanel;
    private JBTextField nameTextField;
    private JBTextField userTextField;
    private JBPasswordField secretTextField;
    private DBNComboBox<AIProvider> providerComboBox;
    private JLabel userLabel;
    private JLabel secretLabel;
    private DBNHyperlinkLabel guideHyperlink;


    private final OciConfigForm ociConfigForm;
    private final AssistantCredential credential;
    private final Set<String> usedNames;
    private boolean generatedName;

    AssistantCredentialEditForm(AssistantCredentialEditDialog parent, Set<String> usedNames) {
        super(parent);
        this.credential = parent.getCredential();
        this.usedNames = usedNames;
        this.generatedName = Strings.isEmpty(credential.getName());
        initCredentialName();

        this.ociConfigForm = new OciConfigForm(this, credential);
        this.ociConfigPanel.add(ociConfigForm.getComponent());
        initComboBox(providerComboBox, getProviders());
        resetFormChanges();

        initFieldAvailability();
        updateFields();

        // listeners
        onSelectionChange(providerComboBox, p -> updateFields());
        onTextChange(nameTextField, e -> generatedName = false);
    }

    private void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isNewCredential(), array(providerComboBox));
        fieldAdapter.initFieldsVisibility(() -> isFieldSupported(USER), array(userLabel, userTextField));
        fieldAdapter.initFieldsVisibility(() -> isSecretFieldSupported(), array(secretLabel, secretTextField));
    }

    private void initCredentialName() {
        if (!generatedName) return;

        AIProvider provider = getSelectedProvider();
        String baseName = provider == null ? "Credential" : provider.getName();

        String name = nextNumberedIdentifier(baseName + " 1", true, () -> usedNames);
        setTextSilently(nameTextField, name);
    }

    private static List<AIProvider> getProviders() {
        List<AIProvider> providers = new ArrayList<>();
        providers.add(null);
        providers.addAll(AIProviderData.getProviders(AssistantType.PUBLIC));
        return providers;
    }

    private boolean isNewCredential() {
        AssistantCredentialEditDialog dialog = ensureParentComponent();
        return dialog.isNewCredential();
    }

    private boolean isFieldSupported(Field field) {
        return getAuthentication().isSupported(field);
    }

    private boolean isSecretFieldSupported() {
        return isFieldSupported(API_KEY) || isFieldSupported(PASSWORD) || isFieldSupported(Field.TOKEN);
    }

    private boolean isOciFieldSupported() {
        return getSelectedProviderId() == AIProviderId.OCI_GEN_AI;
    }


    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmpty(n), "Please provide a credential name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "The credential name is already in use");
        addTextValidation(secretTextField, s -> isNotEmpty(s), "Please provide a credential");
    }

    private void updateFields() {
        initCredentialName();

        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.updateFieldsVisibility();
        fieldAdapter.updateFieldsAvailability();
        ociConfigPanel.setVisible(isOciFieldSupported());

        AIAuthentication authentication = getAuthentication();
        Field secretField = authentication.getSecretField();
        secretLabel.setText(secretField.getName());

        AIProvider provider = getSelectedProvider();
        boolean infoAvailable = provider != null && secretField == API_KEY;
        guideHyperlink.setVisible(infoAvailable);
        if (infoAvailable) {
            String providerName = provider.getName();
            guideHyperlink.setHyperlinkText(providerName + " API keys");
            guideHyperlink.setHyperlinkTarget(provider.getUrl(ProviderUrlType.KEYS));
        }
    }

    private AIAuthentication getAuthentication() {
        AIProvider provider = getSelectedProvider();
        return provider == null ?
                AIAuthentication.USER_PASSWORD :
                provider.getAuthentication();
    }

    @Nullable
    private AIProviderId getSelectedProviderId() {
        AIProvider provider = getSelectedProvider();
        return provider == null ? null : provider.getId();
    }

    @Nullable
    private  AIProvider getSelectedProvider() {
        return getSelection(providerComboBox);
    }

    public void applyFormChanges() {
        credential.setName(getText(nameTextField));
        credential.setUser(getText(userTextField));
        credential.setSecret(secretTextField.getPassword());
        ociConfigForm.applyFormChanges();

        AIProvider provider = getSelectedProvider();
        credential.setProviderId(provider == null ? null : provider.getId());
    }

    public void resetFormChanges() {
        setText(nameTextField, credential.getName());
        setText(userTextField, credential.getUser());
        setText(secretTextField, Chars.toString(credential.getSecret()));
        ociConfigForm.resetFormChanges();

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, credential.getProviderId());
        setSelection(providerComboBox, provider);
    }

    private boolean isNotUsed(String name) {
        return !usedNames.contains(name);
    }

    public String getCredentialName() {
        return getText(nameTextField);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
