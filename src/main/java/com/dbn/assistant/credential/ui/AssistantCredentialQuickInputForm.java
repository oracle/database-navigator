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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.ProviderUrlType;
import com.dbn.common.text.MimeType;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.getText;

public class AssistantCredentialQuickInputForm extends DBNFormBase {
    private JPanel hintPanel;
    private JPanel mainPanel;
    private JBTextField userTextField;
    private JBPasswordField keyPasswordField;
    private DBNHyperlinkLabel guideHyperlink;
    private JLabel userLabel;


    private final AIProvider provider;
    private final @Getter AssistantCredential credential;

    AssistantCredentialQuickInputForm(AssistantCredentialQuickInputDialog parent, AIProvider provider) {
        super(parent);
        this.provider = provider;

        this.credential = new AssistantCredential();
        this.credential.setName(provider.getName());
        this.credential.setProviderId(provider.getId());

        // todo show for providers requiring user input
        this.userLabel.setVisible(false);
        this.userTextField.setVisible(false);

        initHintPanel();
        initGuideHyperlink();
    }

    private void initHintPanel() {
        String providerName = provider.getName();
        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        String ideName = applicationInfo.getVersionName();

        TextContent hintContent = new TextContent("To connect with " + providerName + " language models, please enter your personal API key below. " +
                "You can create and manage your keys on the official " + providerName + " API key page.\n\n" +
                "Your key will be safely stored in the password manager of " + ideName + ".", MimeType.TEXT_PLAIN);
        DBNHintForm hintForm = new DBNHintForm(this, hintContent, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initGuideHyperlink() {
        String providerName = provider.getName();

        guideHyperlink.setHyperlinkText(providerName + " API keys");
        guideHyperlink.setHyperlinkTarget(provider.getUrl(ProviderUrlType.KEYS));
    }

    private static List<AIProvider> getProviders() {
        return AIProviderData.getProviders(AssistantType.PUBLIC);
    }

    @Override
    protected void initValidation() {
        addTextValidation(keyPasswordField, Strings::isNotEmpty, "Please provide an API key");
    }

    public void applyFormChanges() {
        credential.setUser(getText(userTextField));
        credential.setKey(keyPasswordField.getPassword());
        credential.updateSecrets(null);
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
