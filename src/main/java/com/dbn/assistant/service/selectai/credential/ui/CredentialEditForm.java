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

package com.dbn.assistant.service.selectai.credential.ui;

import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.impl.DBCredentialImpl;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBCredentialType;
import com.dbn.oci.config.OciConfig;
import com.dbn.oci.config.OciConfigManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isAlphanumericWithUnderscore;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.startsWith;
import static com.dbn.object.type.DBAttributeType.FINGERPRINT;
import static com.dbn.object.type.DBAttributeType.PASSWORD;
import static com.dbn.object.type.DBAttributeType.PRIVATE_KEY;
import static com.dbn.object.type.DBAttributeType.TENANCY_OCID;
import static com.dbn.object.type.DBAttributeType.USER_NAME;
import static com.dbn.object.type.DBAttributeType.USER_OCID;

/**
 * A dialog window for creating new AI credentials.
 * This window allows users to input credential information, supporting different types of credentials.
 */
@Getter
public class CredentialEditForm extends DBNFormBase {

    private JPanel mainPanel;
    private JPanel attributesPane;
    private JTextField credentialNameField;
    private JCheckBox statusCheckBox;
    private JComboBox<DBCredentialType> credentialTypeComboBox;

    private JTextField passwordCredentialUserField;
    private JPasswordField passwordCredentialPasswordField;
    private JPasswordField tokenCredentialPasswordField;

    private JBTextField ociCredentialUserOcidField;
    private JBTextField ociCredentialTenancyOcidField;
    private JTextField ociCredentialPrivateKeyField;
    private JTextField ociCredentialFingerprintField;

    private JPanel passwordCredentialPanel;
    private JPanel tokenCredentialPanel;
    private JPanel ociCredentialPanel;
    private JButton ociConfigFileButton;

    private DBCredential credential;
    private final ConnectionRef connection;
    private final Set<String> usedCredentialNames;
    private final List<DBCredentialType> credentialTypes;

    /**
     * Constructs a CredentialEditForm
     *
     * @param dialog              the parent dialog
     * @param credential          the credential to be edited, can be null in case of credential creation
     * @param usedCredentialNames the names of credentials which are already defined and name can no longer be used
     */
    public CredentialEditForm(CredentialEditDialog dialog, @Nullable DBCredential credential, List<DBCredentialType> credentialTypes, Set<String> usedCredentialNames) {
        super(dialog);
        this.connection = ConnectionRef.of(dialog.getConnection());
        this.credential = credential;
        this.credentialTypes = credentialTypes == null ? List.of(DBCredentialType.values()) : credentialTypes;
        this.usedCredentialNames = usedCredentialNames;

        initCredentialTypeComboBox();
        initCredentialAttributeFields();
        onButtonClick(ociConfigFileButton, e -> openOciConfigSelector());
    }

    private void openOciConfigSelector() {
        Project project = ensureProject();
        OciConfigManager configManager = OciConfigManager.getInstance(project);
        configManager.openOciConfigSelector(c -> applyOciConfiguration(c));
    }

    private void applyOciConfiguration(OciConfig config) {
        setText(ociCredentialUserOcidField, config.getUserId());
        setText(ociCredentialTenancyOcidField, config.getTenancyId());
        setText(ociCredentialPrivateKeyField, config.getPrivateKeyFile());
        setText(ociCredentialFingerprintField, config.getFingerprint());
        ociCredentialUserOcidField.requestFocus();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private ConnectionHandler getConnection() {
        return connection.ensure();
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
        addTextValidation(credentialNameField, c -> isNotEmpty(c), txt("cfg.assistant.error.CredentialNameEmpty"));
        addTextValidation(credentialNameField, c -> isNotUsed(c), txt("cfg.assistant.error.CredentialNameExists"));
        addTextValidation(credentialNameField, c -> isAlphanumericWithUnderscore(c), txt("cfg.assistant.error.CredentialNameInvalid"));

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

    private void initCredentialTypeComboBox() {
        initComboBox(credentialTypeComboBox, credentialTypes);
        if (credentialTypes.size() == 1 || credential != null) {
            DBCredentialType credentialType = credential == null ? credentialTypes.iterator().next() : credential.getType();
            credentialTypeComboBox.setSelectedItem(credentialType);
            credentialTypeComboBox.setEnabled(false);
            updateFieldAvailability();
        } else {
            credentialTypeComboBox.addActionListener((e) -> updateFieldAvailability());
        }

        ociCredentialUserOcidField.getEmptyText().setText("ocid1.user.oc1...");
        ociCredentialTenancyOcidField.getEmptyText().setText("ocid1.tenancy.oc1...");
    }

    /**
     * Populate fields with the attributes of the credential to be updated
     */
    private void initCredentialAttributeFields() {
        if (credential == null) return;

        setText(credentialNameField, credential.getName());
        credentialNameField.setEnabled(false);
        statusCheckBox.setSelected(credential.isEnabled());
        DBCredentialType credentialType = credential.getType();
        if (credentialType == DBCredentialType.PASSWORD) {
            initPasswordCredentialFields();
        } else if (credentialType == DBCredentialType.TOKEN) {
            initTokenCredentialFields();
        } else if (credentialType == DBCredentialType.OCI) {
            initOciCredentialFields();
        }
    }

    private void initTokenCredentialFields() {
        setSelection(credentialTypeComboBox, DBCredentialType.TOKEN);
    }

    private void initPasswordCredentialFields() {
        setSelection(credentialTypeComboBox, DBCredentialType.PASSWORD);
        setText(passwordCredentialUserField, credential.getUserName());
    }

    private void initOciCredentialFields() {
        setSelection(credentialTypeComboBox, DBCredentialType.OCI);
        setText(ociCredentialUserOcidField, credential.getUserName());
    }

    /**
     * Collects the fields' info and sends them to the service layer to create new credential
     */
    protected void doCreateAction(OutcomeHandler successHandler) {
        credential = inputsToCredential();
        if (credential == null) return;
        getManagementService().createObject(credential, successHandler);
    }

    /**
     * Collects the fields' info and sends them to the service layer to update new credential
     */
    protected void doUpdateAction(OutcomeHandler successHandler) {
        credential = inputsToCredential();
        if (credential == null) return;
        getManagementService().updateObject(credential, successHandler);
    }

    @NotNull
    private ObjectManagementService getManagementService() {
        return ObjectManagementService.getInstance(ensureProject());
    }

    @Nullable
    @SneakyThrows
    private DBCredential inputsToCredential() {
        DBCredentialType credentialType = getCredentialType();
        if (credentialType == null) return null;

        DBSchema schema = getConnection().getObjectBundle().getUserSchema();
        String credentialName = getText(credentialNameField);
        boolean selected = statusCheckBox.isSelected();

        DBCredential credential = new DBCredentialImpl(schema, credentialName, credentialType, selected);
        if (credentialType == DBCredentialType.PASSWORD) {
            credential.setAttribute(USER_NAME, getText(passwordCredentialUserField));
            credential.setAttribute(PASSWORD, getText(passwordCredentialPasswordField));

        } else if (credentialType == DBCredentialType.TOKEN) {
            // special case of credentials created for the vector framework
            credential.setAttribute(USER_NAME, "access_token");
            credential.setAttribute(PASSWORD, getText(tokenCredentialPasswordField));

        } else if (credentialType == DBCredentialType.OCI) {
            credential.setAttribute(USER_OCID, getText(ociCredentialUserOcidField));
            credential.setAttribute(TENANCY_OCID, getText(ociCredentialTenancyOcidField));
            credential.setAttribute(PRIVATE_KEY, getText(ociCredentialPrivateKeyField));
            credential.setAttribute(FINGERPRINT, getText(ociCredentialFingerprintField));

        }
        return credential;
    }

    @Nullable
    private DBCredentialType getCredentialType() {
        return getSelection(credentialTypeComboBox);
    }
}
