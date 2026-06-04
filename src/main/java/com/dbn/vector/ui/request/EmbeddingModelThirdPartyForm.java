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

package com.dbn.vector.ui.request;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Strings;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.request.EmbeddingModelThirdPartySpec;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class EmbeddingModelThirdPartyForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JBTextField urlTextField;
    private JBTextField modelTextField;
    private JLabel providerLabel;
    private JLabel credentialLabel;
    private JLabel urlLabel;
    private JLabel modelLabel;
    private JLabel credentialSchemaLabel;
    private DBObjectSelector<DBSchema> credentialSchemaComboBox;
    private DBObjectSelector<DBCredential> credentialComboBox;
    private DBNComboBox<AIProvider> providerComboBox;
    private JPanel hyperLinkPanel;

    public EmbeddingModelThirdPartyForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        initComboBoxes();
        initDocumentationLink();
    }

    private void initDocumentationLink() {
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "",
                "Supported Third-Party Providers",
                "https://docs.oracle.com/en/database/oracle/oracle-database/26/vecse/supported-third-party-provider-operations-and-endpoints.html");

        hyperLinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.WEST);
    }

    private void initComboBoxes() {
        List<AIProvider> providers = AIProviderData.getProviders(AssistantType.VECTOR_AI);
        ComboBoxes.initComboBox(providerComboBox, providers);

        EmbeddingModelThirdPartySpec config = getConfig();

        credentialSchemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getCredentialSchemaName())
                .triggerLoad();

        credentialComboBox
                .initialize(this, CREDENTIAL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadCredentials())
                .withValuePreselector(() -> config.getCredentialName())
                .withObjectFactory("New Credential...")
                .triggerLoad();

        updateFieldAvailability();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(credentialComboBox));
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(credentialSchemaComboBox, s -> populateCredentials());
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(credentialSchemaComboBox, txt("msg.vector.error.SelectCredentialSchema"));
        addSelectionValidation(credentialComboBox, txt("msg.vector.error.SelectOrCreateCredential"));
        addSelectionValidation(providerComboBox, txt("msg.vector.error.EmbeddingModelProviderRequired"));
        addTextValidation(urlTextField, t -> Strings.isNotEmpty(t), txt("msg.vector.error.EmbeddingModelUrlRequired"));
        addTextValidation(modelTextField, t -> Strings.isNotEmpty(t), txt("msg.vector.error.EmbeddingModelNameRequired"));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public String getProviderApiName() {
        AIProvider provider = getProvider();
        return provider == null ? null : provider.getApiName();
    }

    public @Nullable AIProvider getProvider() {
        return getSelection(providerComboBox);
    }

    public String getUrl() {
        return urlTextField.getText();
    }

    public String getModelName() {
        return modelTextField.getText();
    }

    public EmbeddingModelThirdPartySpec getConfig() {
        return getEmbeddingRequest().getModelConfig().getThirdPartyModelConfig();
    }

    @Override
    public DBSchema getSelectedSchema() {
        return getSelection(credentialSchemaComboBox);
    }

    private void populateCredentials() {
        updateFieldAvailability();
        credentialComboBox.reloadValues();
    }

    private List<DBCredential> loadCredentials() {
        DBSchema schema = getSelectedSchema();
        if (schema == null) return emptyList();

        return schema.getCredentials();
    }

    @Override
    public void resetFormChanges() {
        EmbeddingModelThirdPartySpec config = getConfig();
        setSelection(providerComboBox, getProvider(config.getProvider()));
        modelTextField.setText(config.getModelName());
        urlTextField.setText(config.getEndpointUrl());
        initComboBoxes();
    }

    private static @Nullable AIProvider getProvider(String apiName) {
        return AIProviderData.getProvider(AssistantType.VECTOR_AI, p -> p.getApiName().equals(apiName));
    }

    @Override
    public void applyFormChanges() {
        EmbeddingModelThirdPartySpec config = getConfig();
        config.setProvider(getProviderApiName());
        config.setModelName(getText(modelTextField));
        config.setEndpointUrl(getText(urlTextField));
        config.setCredentialSchemaName(getSelectedObjectName(credentialSchemaComboBox, config.getCredentialSchemaName()));
        config.setCredentialName(getSelectedObjectName(credentialComboBox, config.getCredentialName()));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(providerLabel, providerComboBox);
        alignerData.registerFieldGroup(modelLabel, modelTextField);
        alignerData.registerFieldGroup(urlLabel, urlTextField);
        alignerData.registerFieldGroup(credentialSchemaLabel, credentialSchemaComboBox);
        alignerData.registerFieldGroup(credentialLabel, credentialComboBox);
    }
}
