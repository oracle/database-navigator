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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.provider.ProviderUrlType;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.dbn.oci.ui.OciConfigForm;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;

public class AssistantCredentialQuickInputForm extends DBNFormBase {
    private JPanel hintPanel;
    private JPanel mainPanel;
    private JBTextField userTextField;
    private JBPasswordField keyPasswordField;
    private DBNHyperlinkLabel guideHyperlink;
    private JLabel userLabel;
    private JPanel ociConfigPanel;
    private JPanel userPasswordPanel;

    private final AIProvider provider;
    private final @Getter AssistantCredential credential;
    private final OciConfigForm ociConfigForm;

    AssistantCredentialQuickInputForm(AssistantCredentialQuickInputDialog parent, AIProvider provider) {
        super(parent);
        this.provider = provider;

        this.credential = new AssistantCredential();
        this.credential.setName(provider.getName());
        this.credential.setProviderId(provider.getId());

        // todo show for providers requiring user input
        this.userLabel.setVisible(false);
        this.userTextField.setVisible(false);

        this.ociConfigForm = new OciConfigForm(this, credential);
        this.ociConfigPanel.add(ociConfigForm.getComponent());

        boolean ociCredential = provider.getId() == AIProviderId.OCI_GEN_AI;
        this.ociConfigPanel.setVisible(ociCredential);
        this.userPasswordPanel.setVisible(!ociCredential);
        resetFormChanges();

        initHintPanel();
        initGuideHyperlink();
    }

    private void initHintPanel() {
        String providerName = provider.getName();
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        String ideName = applicationInfo.getVersionName();

        TextContent hintContent = TextContent.plain("To connect with \"" + providerName + "\" language models, please enter your personal API key below. " +
                "You can create and manage your keys on the official \"" + providerName + "\" API key page.\n\n" +
                "Your key will be safely stored in the password manager of " + ideName + ".");
        DBNHintForm hintForm = new DBNHintForm(this, hintContent, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initGuideHyperlink() {
        String providerName = provider.getName();

        guideHyperlink.setHyperlinkText(providerName + " API keys");
        guideHyperlink.setHyperlinkTarget(provider.getUrl(ProviderUrlType.KEYS));
    }

    @Override
    protected void initValidation() {
        addTextValidation(keyPasswordField, Strings::isNotEmpty, "Please provide an API key");
    }

    @Override
    public void resetFormChanges() {
        setText(userTextField, credential.getUser());
        setText(keyPasswordField, Chars.toString(credential.getSecret()));
        ociConfigForm.resetFormChanges();
    }

    public void applyFormChanges() {
        credential.setUser(getText(userTextField));
        credential.setSecret(keyPasswordField.getPassword());
        credential.updateSecrets(null);

        ociConfigForm.applyFormChanges();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return keyPasswordField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
