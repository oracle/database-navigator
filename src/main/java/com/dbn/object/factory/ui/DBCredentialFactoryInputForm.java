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

package com.dbn.object.factory.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBCredentialType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.HashSet;
import java.util.Set;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setPassword;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isAlphanumericWithUnderscore;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.startsWith;
import static com.dbn.object.factory.model.DBObjectAttributeType.CREDENTIAL_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.FINGERPRINT;
import static com.dbn.object.factory.model.DBObjectAttributeType.PASSWORD;
import static com.dbn.object.factory.model.DBObjectAttributeType.PRIVATE_KEY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TENANCY_OCID;
import static com.dbn.object.factory.model.DBObjectAttributeType.USER_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.USER_OCID;

@Getter
public class DBCredentialFactoryInputForm extends DBSchemaObjectFactoryInputForm<DBObjectSpec> {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNComboBox connectionComboBox;
    private DBNComboBox schemaComboBox;
    private JTextField nameTextField;

    private JPanel attributesPanel;
    private JPanel passwordCredentialPanel;
    private JPanel ociCredentialPanel;
    private JPanel tokenCredentialPanel;
    private JTextField passwordCredentialUserField;
    private JTextField ociCredentialPrivateKeyField;
    private JTextField ociCredentialFingerprintField;
    private JBTextField ociCredentialUserOcidField;
    private JBTextField ociCredentialTenancyOcidField;
    private JPasswordField passwordCredentialPasswordField;
    private JPasswordField tokenCredentialPasswordField;
    private JComboBox<DBCredentialType> credentialTypeComboBox;


    private final Set<String> usedCredentialNames = new HashSet<>(); // TODO

    public DBCredentialFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);

        initHeaderForm();
        initContextComponents();
        initCredentialTypes();

        resetFormChanges();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getCredentialType() == DBCredentialType.TOKEN, array(tokenCredentialPanel));
        fieldAdapter.initFieldsVisibility(() -> getCredentialType() == DBCredentialType.PASSWORD, array(passwordCredentialPanel));
        fieldAdapter.initFieldsVisibility(() -> getCredentialType() == DBCredentialType.OCI, array(ociCredentialPanel));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, c -> isNotEmpty(c), txt("cfg.assistant.error.CredentialNameEmpty"));
        addTextValidation(nameTextField, c -> isNotUsed(c), txt("cfg.assistant.error.CredentialNameExists"));
        addTextValidation(nameTextField, c -> isAlphanumericWithUnderscore(c), txt("cfg.assistant.error.CredentialNameInvalid"));

        addTextValidation(passwordCredentialUserField, c -> !isPassword() || isNotEmpty(c), txt("cfg.assistant.error.UserNameEmpty"));
        addTextValidation(passwordCredentialPasswordField, c -> !isPassword() || isNotEmpty(c), txt("cfg.assistant.error.PasswordEmpty"));
        addTextValidation(tokenCredentialPasswordField, c -> !isToken() || isNotEmpty(c), txt("cfg.assistant.error.TokenEmpty"));

        addTextValidation(ociCredentialUserOcidField, c -> !isOci() || isNotEmpty(c), txt("cfg.assistant.error.UserOcidEmpty"));
        addTextValidation(ociCredentialUserOcidField, c -> !isOci() || startsWith(c, "ocid1.user.oc1."), txt("cfg.assistant.error.UserOcidInvalid"));
        addTextValidation(ociCredentialTenancyOcidField, c -> !isOci() || isNotEmpty(c), txt("cfg.assistant.error.UserTenancyOcidEmpty"));
        addTextValidation(ociCredentialTenancyOcidField, c -> !isOci() || startsWith(c, "ocid1.tenancy.oc1."), txt("cfg.assistant.error.UserTenancyOcidInvalid"));
        addTextValidation(ociCredentialFingerprintField, c -> !isOci() || isNotEmpty(c), txt("cfg.assistant.error.FingerprintEmpty"));
        addTextValidation(ociCredentialPrivateKeyField, c -> !isOci() || isNotEmpty(c), txt("cfg.assistant.error.PrivateKeyEmpty"));
    }

    private boolean isPassword() {
        return getCredentialType() == DBCredentialType.PASSWORD;
    }

    private boolean isToken() {
        return getCredentialType() == DBCredentialType.TOKEN;
    }

    private boolean isOci() {
        return getCredentialType() == DBCredentialType.OCI;
    }

    private boolean isNotUsed(String name) {
        return !usedCredentialNames.contains(name);
    }

    private void initCredentialTypes() {
        DBCredentialType[] credentialTypes = DBCredentialType.values();
        initComboBox(credentialTypeComboBox, credentialTypes);
        if (credentialTypes.length == 1) {
            DBCredentialType credentialType = credentialTypes[0];
            credentialTypeComboBox.setSelectedItem(credentialType);
            credentialTypeComboBox.setEnabled(false);
            updateFieldAvailability();
        } else {
            credentialTypeComboBox.addActionListener((e) -> updateFieldAvailability());
        }

        ociCredentialUserOcidField.getEmptyText().setText("ocid1.user.oc1...");
        ociCredentialTenancyOcidField.getEmptyText().setText("ocid1.tenancy.oc1...");
    }

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();

        DBCredentialType credentialType = CREDENTIAL_TYPE.of(input);
        setSelection(credentialTypeComboBox, credentialType);

        if (credentialType == null) return;

        switch (credentialType) {
            case PASSWORD -> {
                setText(passwordCredentialUserField, USER_NAME.of(input));
                setPassword(passwordCredentialPasswordField, PASSWORD.of(input));
            }
            case TOKEN -> {
                // special case of credentials created for the vector framework
                setPassword(tokenCredentialPasswordField, PASSWORD.of(input));
            }
            case OCI -> {
                setText(ociCredentialUserOcidField, USER_OCID.of(input));
                setText(ociCredentialTenancyOcidField, TENANCY_OCID.of(input));
                setText(ociCredentialPrivateKeyField, PRIVATE_KEY.of(input));
                setText(ociCredentialFingerprintField, FINGERPRINT.of(input));
            }
        }

    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();

        DBCredentialType credentialType = getCredentialType();
        input.setAttributeValue(CREDENTIAL_TYPE, credentialType);

        if (credentialType == null) return;
        switch (credentialType) {
            case PASSWORD -> {
                input.setAttributeValue(USER_NAME, getText(passwordCredentialUserField));
                input.setAttributeValue(PASSWORD, passwordCredentialPasswordField.getPassword());
            }
            case TOKEN -> {
                // special case of credentials created for the vector framework
                input.setAttributeValue(USER_NAME, "access_token");
                input.setAttributeValue(PASSWORD, tokenCredentialPasswordField.getPassword());
            }
            case OCI -> {
                input.setAttributeValue(USER_OCID, getText(ociCredentialUserOcidField));
                input.setAttributeValue(TENANCY_OCID, getText(ociCredentialTenancyOcidField));
                input.setAttributeValue(PRIVATE_KEY, getText(ociCredentialPrivateKeyField));
                input.setAttributeValue(FINGERPRINT, getText(ociCredentialFingerprintField));
            }
        }
    }

    @Nullable
    private DBCredentialType getCredentialType() {
        return getSelection(credentialTypeComboBox);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
