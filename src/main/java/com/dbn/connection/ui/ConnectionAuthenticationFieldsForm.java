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

package com.dbn.connection.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.form.field.JComponentCategory;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Sockets;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.oci.config.OciConfigFileUtil;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.field.JComponentFilter.accessibleClassifiedAs;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.form.field.JComponentFilter.classifiedAs;
import static com.dbn.common.ui.form.field.JComponentFilter.inaccessible;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.isPasswordChanged;
import static com.dbn.common.ui.util.PasswordFields.setPassword;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.connection.AuthenticationTokenType.ALL_AZURE_TOKEN_TYPES;
import static com.dbn.connection.AuthenticationTokenType.AZURE_INTERACTIVE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_TOKEN;
import static com.dbn.connection.AuthenticationTokenType.OCI_API_KEY;
import static com.dbn.connection.AuthenticationTokenType.OCI_INTERACTIVE;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.connection.ui.ConnectionAuthenticationFieldsForm.FieldCategory.CACHEABLE_FIELDS;
import static com.dbn.database.oracle.OracleCompatibilityInterface.ProviderErrorHandlingConstants.OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionAuthenticationFieldsForm extends DBNFormBase {

    enum FieldCategory implements JComponentCategory {
        CACHEABLE_FIELDS,
    }

    private static final boolean IS_PROXY_MAYBE_SET = checkIfHttpProxy();
    private JComboBox<AuthenticationType> authTypeComboBox;
    private JComboBox<AuthenticationTokenType> tokenTypeComboBox;
    private DBNComboBox<String> tokenProfileComboBox;
    private TextFieldWithBrowseButton tokenConfigFileTextField;
    private JTextField userTextField;
    private JPasswordField passwordField;
    private JPanel mainPanel;
    private JLabel userLabel;
    private JLabel passwordLabel;
    private JLabel tokenTypeLabel;
    private JLabel tokenConfigFileLabel;
    private JLabel tokenProfileLabel;
    private JPanel warningPanel;

    private JLabel compartmentOcidLabel;
    private JLabel databaseOcidLabel;

    private JLabel azureAppIdUriLabel;
    private JLabel azureClientCertificateFileLabel;
    private JLabel azureTenantIdLabel;
    private JLabel azureClientIdLabel;
    private JLabel azureClientSecretLabel;
    private JLabel azureClientCertificatePasswordLabel;

    private JTextField compartmentOcidTextField;
    private JTextField databaseOcidTextField;

    private JTextField azureClientIdTextField;
    private JTextField azureTenantIdTextField;
    private JTextField azureAppIdUriTextField;
    private TextFieldWithBrowseButton azureClientCertificateFileTextField;
    private JPasswordField azureClientSecretPasswordField;
    private JPasswordField azureClientCertificateFilePasswordField;
    private DBNHintForm warningHintForm;


    public ConnectionAuthenticationFieldsForm(@NotNull DBNForm parentComponent) {
        super(parentComponent);

        addSingleFileChooser(
                getProject(), tokenConfigFileTextField,
                txt("cfg.oci.title.SelectConfigFile"),
                txt("cfg.oci.text.ValidOciConfigFile"));
        onTextChange(tokenConfigFileTextField, e -> tokenProfileComboBox.reloadValues());

        initComboBox(authTypeComboBox, AuthenticationType.values());
        // currently supported token types
        initComboBox(tokenTypeComboBox,
                OCI_API_KEY,
                OCI_INTERACTIVE,
                AZURE_SERVICE_PRINCIPAL_CERTIFICATE,
                AZURE_SERVICE_PRINCIPAL_TOKEN,
                AZURE_INTERACTIVE);

        onSelectionChange(authTypeComboBox, v -> updateFieldAvailability());
        onSelectionChange(tokenTypeComboBox, v -> updateFieldAvailability());

        this.warningHintForm = new DBNHintForm(this, null, MessageType.WARNING, true);
        warningPanel.add(warningHintForm.getComponent());
        warningPanel.setVisible(false);

        addSingleFileChooser(
                getProject(), azureClientCertificateFileTextField,
                txt("cfg.connection.title.SelectAzureClientCertificateFile"),
                txt("cfg.connection.text.AzureClientCertificateFile"));
        onTextChange(azureClientCertificateFileTextField, e -> refreshAzureClientCertificateFile());

    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();

        // init visibility conditions
        fieldAdapter.initFieldsVisibility(() -> isUserAuth(), array(userLabel, userTextField));
        fieldAdapter.initFieldsVisibility(() -> isPasswordAuth(), array(passwordLabel, passwordField));

        fieldAdapter.initFieldsVisibility(() -> isTokenAuth(), array(
                tokenTypeLabel,
                tokenTypeComboBox,
                tokenConfigFileLabel,
                tokenConfigFileTextField,
                tokenProfileLabel,
                tokenProfileComboBox));

        fieldAdapter.initFieldsVisibility(() -> isOciTokenAuth(), array(
                compartmentOcidLabel,
                compartmentOcidTextField,
                databaseOcidLabel,
                databaseOcidTextField));

        fieldAdapter.initFieldsVisibility(() -> isApiKeyTokenAuth(), array(
                tokenConfigFileLabel,
                tokenConfigFileTextField,
                tokenProfileLabel,
                tokenProfileComboBox));

        fieldAdapter.initFieldsVisibility(() -> isAzureTokenAuth(), array (
                azureAppIdUriLabel,
                azureAppIdUriTextField));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipal(), array (
                azureClientIdLabel,
                azureClientIdTextField,
                azureTenantIdLabel,
                azureTenantIdTextField));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipalCertAuth(), array (
                azureClientCertificateFileLabel,
                azureClientCertificateFileTextField,
                azureClientCertificatePasswordLabel,
                azureClientCertificateFilePasswordField));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipalSecretAuth(), array(
                azureClientSecretLabel,
                azureClientSecretPasswordField));

        // init field classification
        fieldAdapter.classifyFields(CACHEABLE_FIELDS, array(
                userTextField,
                passwordField,
                tokenTypeComboBox,
                tokenConfigFileTextField,
                tokenProfileComboBox,
                azureClientIdTextField,
                azureTenantIdTextField,
                azureClientCertificateFileLabel,
                azureAppIdUriTextField,
                azureClientSecretPasswordField,
                compartmentOcidTextField,
                databaseOcidTextField));
    }

    protected void updateFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();

        // cache values of fields classified as CACHEABLE
        fieldAdapter.captureFieldValues(classifiedAs(CACHEABLE_FIELDS));
        fieldAdapter.updateFieldsVisibility();
        fieldAdapter.updateFieldsAvailability();
        fieldAdapter.resetFieldValues(inaccessible());

        // restore values of fields classified as CACHEABLE which are visible and enabled
        fieldAdapter.restoreFieldValues(accessibleClassifiedAs(CACHEABLE_FIELDS));

        // monitor auth type changes and fire a warning
        // event under Bug_38087045 conditions.
        Dispatch.async(mainPanel,
            () -> checkSystemWarnings(),
            warningMessage -> {
                if (warningMessage != null) {
                    warningHintForm.setHintContent(plain(warningMessage));
                }
                warningPanel.setVisible(warningMessage != null);
            }
        );
    }

    public void setAuthenticationTypes(AuthenticationType ...  authenticationTypes) {
        initComboBox(authTypeComboBox, authenticationTypes);
    }

    public void addChangeListeners(Runnable runnable) {
        onTextChange(userTextField, e -> runnable.run());
        onTextChange(passwordField, e -> runnable.run());
        onTextChange(tokenConfigFileTextField, e -> runnable.run());
        onTextChange(compartmentOcidTextField, e -> runnable.run());
        onTextChange(databaseOcidTextField, e -> runnable.run());

        onTextChange(azureClientIdTextField, e -> runnable.run());
        onTextChange(azureTenantIdTextField, e -> runnable.run());
        onTextChange(azureAppIdUriTextField, e -> runnable.run());
        onTextChange(azureClientCertificateFileTextField, e -> runnable.run());
        onTextChange(azureClientCertificateFilePasswordField, e -> runnable.run());
        onTextChange(azureClientSecretPasswordField, e->runnable.run());

        tokenTypeComboBox.addActionListener(e -> runnable.run());
        tokenProfileComboBox.addActionListener(e -> runnable.run());
        authTypeComboBox.addActionListener(e -> runnable.run());
    }

    public void applyFormChanges(AuthenticationInfo authenticationInfo){
        // irrelevant fields are all supposed to be emptied at this stage by resetFieldValues(), if disabled or hidden
        // no auth type check needed here
        authenticationInfo.setType(getSelection(authTypeComboBox));
        authenticationInfo.setUser(getText(userTextField));
        authenticationInfo.setPassword(getPassword(passwordField, authenticationInfo.getPassword()));

        authenticationInfo.setTokenType(getSelection(tokenTypeComboBox));
        authenticationInfo.setTokenProfile(getSelection(tokenProfileComboBox));
        authenticationInfo.setTokenConfigFile(getText(tokenConfigFileTextField));
        authenticationInfo.setCompartmentOcid(getText(compartmentOcidTextField));
        authenticationInfo.setDatabaseOcid(getText(databaseOcidTextField));

        authenticationInfo.setAzureClientId(getText(azureClientIdTextField));
        authenticationInfo.setAzureTenantId(getText(azureTenantIdTextField));
        authenticationInfo.setAzureClientCertificateFile(getText(azureClientCertificateFileTextField));
        authenticationInfo.setAzureClientCertificatePassword(getPassword(azureClientCertificateFilePasswordField, authenticationInfo.getAzureClientCertificatePassword()));
        authenticationInfo.setAzureDatabaseApplicationIdUri(getText(azureAppIdUriTextField));
        authenticationInfo.setAzureClientSecret(getPassword(azureClientSecretPasswordField, authenticationInfo.getAzureClientSecret()));
    }

    public void resetFormChanges(AuthenticationInfo authenticationInfo) {
        setText(userTextField, authenticationInfo.getUser());
        setPassword(passwordField, authenticationInfo.getPassword());
        setSelection(authTypeComboBox, authenticationInfo.getType());

        setText(tokenConfigFileTextField, authenticationInfo.getTokenConfigFile());
        setSelection(tokenProfileComboBox, authenticationInfo.getTokenProfile());
        setSelection(tokenTypeComboBox, authenticationInfo.getTokenType());
        setText(compartmentOcidTextField, authenticationInfo.getCompartmentOcid());
        setText(databaseOcidTextField, authenticationInfo.getDatabaseOcid());

        setText(azureAppIdUriTextField, authenticationInfo.getAzureDatabaseApplicationIdUri());
        setText(azureClientCertificateFileTextField, authenticationInfo.getAzureClientCertificateFile());
        setPassword(azureClientCertificateFilePasswordField, authenticationInfo.getAzureClientCertificatePassword());
        setText(azureClientIdTextField, authenticationInfo.getAzureClientId());
        setText(azureTenantIdTextField, authenticationInfo.getAzureTenantId());
        setPassword(azureClientSecretPasswordField, authenticationInfo.getAzureClientSecret());

        tokenProfileComboBox
                .withValueLoader(() -> loadOciConfigProfiles())
                .withValuePreselector(p -> Objects.equals(p, authenticationInfo.getTokenProfile()))
                .triggerLoad();

        updateFieldAvailability();
    }

    private List<String> loadOciConfigProfiles() {
        String configFilePath = getConfigFilePath();
        return OciConfigFileUtil.getConfigProfileNames(configFilePath);
    }

    private String getConfigFilePath() {
        return getText(tokenConfigFileTextField);
    }


    public boolean settingsChanged(AuthenticationInfo authenticationInfo) {
        return  !Commons.match(authenticationInfo.getType(), getSelection(authTypeComboBox)) ||

                // basic auth
                !Commons.match(authenticationInfo.getUser(), getText(userTextField)) ||
                isPasswordChanged(passwordField, authenticationInfo.getPassword()) ||

                // oci token auth
                !Commons.match(authenticationInfo.getTokenType(), getSelection(tokenTypeComboBox)) ||
                !Commons.match(authenticationInfo.getTokenConfigFile(), getText(tokenConfigFileTextField)) ||
                !Commons.match(authenticationInfo.getTokenProfile(), getSelection(tokenProfileComboBox)) ||
                !Commons.match(authenticationInfo.getDatabaseOcid(), getText(databaseOcidTextField)) ||
                !Commons.match(authenticationInfo.getCompartmentOcid(), getText(compartmentOcidTextField)) ||

                // azure auth
                !Commons.match(authenticationInfo.getAzureClientId(), getText(azureClientIdTextField)) ||
                !Commons.match(authenticationInfo.getAzureTenantId(), getText(azureTenantIdTextField)) ||
                !Commons.match(authenticationInfo.getAzureClientCertificateFile(), getText(azureClientCertificateFileTextField)) ||
                isPasswordChanged(azureClientCertificateFilePasswordField, authenticationInfo.getAzureClientCertificatePassword()) ||
                isPasswordChanged(azureClientSecretPasswordField, authenticationInfo.getAzureClientSecret()) ||
                !Commons.match(authenticationInfo.getAzureDatabaseApplicationIdUri(), getText(azureAppIdUriTextField));
    }

    private void refreshAzureClientCertificateFile() {
        JTextField textField = azureClientCertificateFileTextField.getTextField();
        TextFields.updateFieldError(textField, null);
        String certificateFileStr = getText(textField);
        File certificateFile = new File(certificateFileStr);
        if (!certificateFile.isFile()) {
            TextFields.updateFieldError(textField,
                txt("cfg.connection.error.CertificateFileNotFound", certificateFileStr));
        }
    }

    /***********************************************************************
     *                          LOOKUP UTILITIES                           *
     ***********************************************************************/

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public @Nullable String getTokenProfile() {
        return getSelection(tokenProfileComboBox);
    }

    private boolean isUserAuth() {
        return Commons.isOneOf(getAuthenticationType(), USER, USER_PASSWORD);
    }

    private boolean isPasswordAuth() {
        return getAuthenticationType() == USER_PASSWORD;
    }

    private boolean isTokenAuth() {
        return getAuthenticationType() == AuthenticationType.TOKEN;
    }

    private boolean isOciTokenAuth() {
        return isApiKeyTokenAuth() || isOciInteractiveAuth();
    }
    private boolean isOciInteractiveAuth() {
        return isTokenAuth() && getTokenAuthenticationType() == OCI_INTERACTIVE;
    }
    private boolean isApiKeyTokenAuth() {
        return isTokenAuth() && getTokenAuthenticationType() == OCI_API_KEY;
    }

    private boolean isAzureTokenAuth() {
        return isTokenAuth() && Commons.isOneOf(getTokenAuthenticationType(), AZURE_SERVICE_PRINCIPAL_CERTIFICATE, AZURE_SERVICE_PRINCIPAL_TOKEN, AZURE_INTERACTIVE);
    }

    private boolean isAzureServicePrincipal() {
        return isAzureTokenAuth() && Commons.isOneOf(getTokenAuthenticationType(), AZURE_SERVICE_PRINCIPAL_CERTIFICATE, AZURE_SERVICE_PRINCIPAL_TOKEN);
    }
    private boolean isAzureServicePrincipalCertAuth() {
        return isTokenAuth() && getTokenAuthenticationType() == AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
    }

    private boolean isAzureServicePrincipalSecretAuth() {
        return isTokenAuth() && getTokenAuthenticationType() == AZURE_SERVICE_PRINCIPAL_TOKEN;
    }
    @Nullable
    private AuthenticationType getAuthenticationType() {
        return getSelection(authTypeComboBox);
    }

    @Nullable
    private AuthenticationTokenType getTokenAuthenticationType() {
        return getSelection(tokenTypeComboBox);
    }

    private @Nullable @Nls String checkSystemWarnings() {
        if (!verifyInteractivePortBinding()) {
            return txt("cfg.connection.warning.OciInteractivePortBound", OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT);
        }
        if (!checkNoProxyIfAzure()) {
            return txt("cfg.connection.warning.AzureAuthenticationProxySettings");
        }
        return null;
    }
    /**
     * Checks if the token callback bind port (8181) can be bound.
     * Used to warn the interactive connectivity option if the port is already bound
     * @return true if the port is free or binding is irrelevant for the selected authentication type, false otherwise
     */
    private boolean verifyInteractivePortBinding() {
        AuthenticationType authenticationType = getAuthenticationType();
        AuthenticationTokenType tokenAuthenticationType = getTokenAuthenticationType();

        if (authenticationType != AuthenticationType.TOKEN) return true;
        if (tokenAuthenticationType != OCI_INTERACTIVE) return true;

        return Sockets.tryToBindPort(OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT);
    }

    /**
     * If http proxy settings are configured by -D option (usually via JAVA_TOOL_OPTIONS),
     * then this cause problems with the mechanisms that the Azure provider uses to
     * acquire valid security tokens over HTTP.
     *
     * @return true if we currently are using an Azure auth method and no HTTP proxies appear
     * to be set via -D.
     */
    private boolean checkNoProxyIfAzure() {
        AuthenticationType authenticationType = getAuthenticationType();
        AuthenticationTokenType tokenAuthenticationType = getTokenAuthenticationType();

        if (authenticationType != AuthenticationType.TOKEN) return true;
        if (!ALL_AZURE_TOKEN_TYPES.contains(tokenAuthenticationType)) return true;

        return !IS_PROXY_MAYBE_SET;
    }

    private static boolean checkIfHttpProxy() {
        Properties properties = System.getProperties();
        return properties.containsKey("http.proxyHost") ||
                properties.containsKey("http.proxyPort") ||
                properties.containsKey("http.nonProxyHosts") ||
                properties.containsKey("https.proxyHost") ||
                properties.containsKey("https.proxyPort") ||
                properties.containsKey("https.nonProxyHosts");
    }
}
