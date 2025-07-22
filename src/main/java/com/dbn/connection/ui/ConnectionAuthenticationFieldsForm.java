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
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.form.field.JComponentCategory;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Sockets;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.field.JComponentFilter.accessibleClassifiedAs;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.form.field.JComponentFilter.classifiedAs;
import static com.dbn.common.ui.form.field.JComponentFilter.inaccessible;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.connection.AuthenticationTokenType.AZURE_INTERACTIVE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_TOKEN;
import static com.dbn.connection.AuthenticationTokenType.OCI_API_KEY;
import static com.dbn.connection.AuthenticationTokenType.OCI_INTERACTIVE;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.connection.ui.ConnectionAuthenticationFieldsForm.FieldCategory.CACHEABLE_FIELDS;
import static com.dbn.database.oracle.OracleCompatibilityInterface.ProviderErrorHandlingConstants.OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT;

public class ConnectionAuthenticationFieldsForm extends DBNFormBase {

    enum FieldCategory implements JComponentCategory {
        CACHEABLE_FIELDS,
    }

    private JComboBox<AuthenticationType> authTypeComboBox;
    private JComboBox<AuthenticationTokenType> tokenTypeComboBox;
    private JComboBox<String> tokenProfileComboBox;
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

    private JTextField azureClientIdTextField;
    private JTextField azureTenantIdTextField;
    private JTextField azureAppIdUriTextField;
    private TextFieldWithBrowseButton azureClientCertificateFileTextField;
    private JLabel azureAppIdUriLabel;
    private JLabel azureClientCertificateFileLabel;
    private JLabel azureTenantIdLabel;
    private JLabel azureClientIdLabel;
    private JPasswordField azureClientSecretPasswordField;
    private JLabel azureClientSecretLabel;
    private JPasswordField azureClientCertificateFilePasswordTextField;
    private JLabel azureClientCertificatePasswordLabel;


    public ConnectionAuthenticationFieldsForm(@NotNull DBNForm parentComponent) {
        super(parentComponent);

        addSingleFileChooser(
                getProject(), tokenConfigFileTextField,
                "Select OCI Configuration File",
                "Folder must contain an oci config file (usually ~/.oci/config)");
        onTextChange(tokenConfigFileTextField, e -> refreshTokenProfileOptions());

        initComboBox(authTypeComboBox, AuthenticationType.values());
        // currently supported token types
        initComboBox(tokenTypeComboBox,
                OCI_API_KEY,
                OCI_INTERACTIVE,
                AZURE_SERVICE_PRINCIPAL_CERTIFICATE,
                AZURE_SERVICE_PRINCIPAL_TOKEN,
                AZURE_INTERACTIVE);

        onSelectionChange(authTypeComboBox, v -> updateAuthenticationFields());
        onSelectionChange(tokenTypeComboBox, v -> updateAuthenticationFields());

        // TODO NLS
        TextContent interactivePortHintText = plain("TCP port 8181 appears to be bound.\nThis may cause interactive OCI authentication to fail.");
        DBNHintForm hintForm = new DBNHintForm(this, interactivePortHintText, MessageType.WARNING, true);
        warningPanel.add(hintForm.getComponent());

        warningPanel.setVisible(false);

        addSingleFileChooser(
                getProject(), azureClientCertificateFileTextField,
                "Select Azure Client Certificate File",
                "File is a certificate file in pem format");
        onTextChange(azureClientCertificateFileTextField, e -> refreshAzureClientCertificateFile());

        initFields();
    }

    private void initFields() {
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

        fieldAdapter.initFieldsVisibility(() -> isApiKeyTokenAuth(), array(
                tokenConfigFileLabel,
                tokenConfigFileTextField,
                tokenProfileLabel,
                tokenProfileComboBox));

        fieldAdapter.initFieldsVisibility(() -> isAzureTokenAuth(), array (
                azureAppIdUriLabel,
                azureAppIdUriTextField
        ));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipal(), array (
                azureClientIdLabel,
                azureClientIdTextField,
                azureTenantIdLabel,
                azureTenantIdTextField
        ));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipalCertAuth(), array (
                azureClientCertificateFileLabel,
                azureClientCertificateFileTextField,
                azureClientCertificatePasswordLabel,
                azureClientCertificateFilePasswordTextField
        ));

        fieldAdapter.initFieldsVisibility(() -> isAzureServicePrincipalSecretAuth(), array(
                azureClientSecretLabel,
                azureClientSecretPasswordField
        ));

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
                azureClientSecretPasswordField));
    }

    private void updateAuthenticationFields() {
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
                () -> verifyInteractivePortBinding(),
                success -> warningPanel.setVisible(!success));
    }

    public void setAuthenticationTypes(AuthenticationType ...  authenticationTypes) {
        initComboBox(authTypeComboBox, authenticationTypes);
    }

    public void addChangeListeners(Runnable runnable) {
        onTextChange(userTextField, e -> runnable.run());
        onTextChange(passwordField, e -> runnable.run());
        onTextChange(tokenConfigFileTextField.getTextField(), e -> runnable.run());

        onTextChange(azureClientIdTextField, e -> runnable.run());
        onTextChange(azureTenantIdTextField, e -> runnable.run());
        onTextChange(azureAppIdUriTextField, e -> runnable.run());
        onTextChange(azureClientCertificateFileTextField, e -> runnable.run());
        onTextChange(azureClientCertificateFilePasswordTextField, e -> runnable.run());
        onTextChange(azureClientSecretPasswordField, e->runnable.run());

        tokenTypeComboBox.addActionListener(e -> runnable.run());
        tokenProfileComboBox.addActionListener(e -> runnable.run());
        authTypeComboBox.addActionListener(e -> runnable.run());
    }

    public void applyFormChanges(AuthenticationInfo authenticationInfo){
        // irrelevant fields are all supposed to be emptied at this stage by resetFieldValues(), if disabled or hidden
        // no auth type check needed here
        authenticationInfo.setType(getSelection(authTypeComboBox));
        authenticationInfo.setUser(userTextField.getText());
        authenticationInfo.setPassword(passwordField.getPassword());

        authenticationInfo.setTokenType(getSelection(tokenTypeComboBox));
        authenticationInfo.setTokenProfile(getSelection(tokenProfileComboBox));
        authenticationInfo.setTokenConfigFile(tokenConfigFileTextField.getText());

        authenticationInfo.setAzureClientId(azureClientIdTextField.getText());
        authenticationInfo.setAzureTenantId(azureTenantIdTextField.getText());
        authenticationInfo.setAzureClientCertificateFile(azureClientCertificateFileTextField.getText());
        authenticationInfo.setAzureClientCertificatePassword(azureClientCertificateFilePasswordTextField.getPassword());
        authenticationInfo.setAzureDatabaseApplicationIdUri(azureAppIdUriTextField.getText());
        authenticationInfo.setAzureClientSecret(azureClientSecretPasswordField.getPassword());
    }

    public void resetFormChanges(AuthenticationInfo authenticationInfo) {
        userTextField.setText(authenticationInfo.getUser());
        passwordField.setText(Chars.toString(authenticationInfo.getPassword()));
        setSelection(authTypeComboBox, authenticationInfo.getType());

        tokenConfigFileTextField.setText(authenticationInfo.getTokenConfigFile());
        setSelection(tokenProfileComboBox, authenticationInfo.getTokenProfile());
        setSelection(tokenTypeComboBox, authenticationInfo.getTokenType());

        azureAppIdUriTextField.setText(authenticationInfo.getAzureDatabaseApplicationIdUri());
        azureClientCertificateFileTextField.setText(authenticationInfo.getAzureClientCertificateFile());
        azureClientCertificateFilePasswordTextField.setText(Chars.toString(authenticationInfo.getAzureClientCertificatePassword()));
        azureClientIdTextField.setText(authenticationInfo.getAzureClientId());
        azureTenantIdTextField.setText(authenticationInfo.getAzureTenantId());
        azureClientSecretPasswordField.setText(Chars.toString(authenticationInfo.getAzureClientSecret()));

        updateAuthenticationFields();
    }

    private void refreshTokenProfileOptions() {
        JTextField textField = tokenConfigFileTextField.getTextField();
        String configFilePath = textField.getText();
        List<String> profiles = Collections.emptyList();
        String selectedProfile = getTokenProfile();
        try {
            // TODO this may take time to load if file is located on a remote location - consider showing a spinner next to the profile dropdown
            //  (is remote config a valid use case anyways?)
            profiles = loadTokenProfiles(configFilePath);
            TextFields.updateFieldError(textField, null);
        } catch (Exception e) {
            TextFields.updateFieldError(textField, e.getMessage());
        }

        selectedProfile = profiles.contains(selectedProfile) ? selectedProfile : firstElement(profiles);
        tokenProfileComboBox.setModel(new DefaultComboBoxModel<>(profiles.toArray(new String[0])));
        tokenProfileComboBox.setSelectedItem(selectedProfile);
    }

    private void refreshAzureClientCertificateFile() {
        JTextField textField = azureClientCertificateFileTextField.getTextField();
        TextFields.updateFieldError(textField, null);
        String certificateFileStr = textField.getText();
        File certificateFile = new File(certificateFileStr);
        if (!certificateFile.isFile()) {
            TextFields.updateFieldError(textField,
                String.format("Can't find the certificate file. %s is not a file", certificateFileStr));
        }
    }
	private List<String> loadTokenProfiles(String configFilePath) {
        if (configFilePath == null) return Collections.emptyList();

        File configFile = new File(configFilePath);
        if (!configFile.exists()) throw new IllegalArgumentException("File does not exist");
        if (!configFile.isFile()) throw new IllegalArgumentException("Path is expected to be a config file");

		List<String> profileEntries = new ArrayList<>();
		try (FileReader fileReader =  new FileReader(configFile);
			    BufferedReader configReader = new BufferedReader(fileReader);)
		{
			String nextLine;
			while ((nextLine = configReader.readLine()) != null) {
				nextLine = nextLine.trim();
                // TODO maybe use regex "\[[a-zA-Z0-9-]+\]"
				if (nextLine.length() > 2) {  // must be '[' and  ']' plus at least on char
					char firstChar = nextLine.charAt(0);
					if (firstChar == '[') {
						final int lastCharIdx = nextLine.length()-1;
						char lastChar = nextLine.charAt(lastCharIdx);
						if (lastChar == ']') {
							// apparently the ConfigParser accepts everything.
							// should we be more protective?
							profileEntries.add(nextLine.substring(1, lastCharIdx));
						}
					}
				}
			}
            if (profileEntries.isEmpty()) throw new IllegalArgumentException("No profile entries found in the given file");
		}
		catch (IOException ioe) {
            throw new IllegalArgumentException("Failed to load config file. Cause: " + ioe.getMessage());
		}

        return profileEntries;
	}

    /***********************************************************************
     *                          LOOKUP UTILITIES                           *
     ***********************************************************************/

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public String getUser() {
        return userTextField.getText();
    }

    public @Nullable String getTokenConfigFile() {
        return tokenConfigFileTextField.getText();
    }

    public @Nullable String getTokenProfile() {
        return (String) tokenProfileComboBox.getSelectedItem();
    }
    public @Nullable String getAzureTokenClientId() {
        return (String) azureClientIdTextField.getText();
    }
    public @Nullable String getAzureTokenTenantId() {
        return (String) azureTenantIdTextField.getText();
    }
    public @Nullable String getAzureTokenClientSecretFile() {
        return (String) azureClientCertificateFileTextField.getText();
    }
    public char[] getAzureTokenClientSecretFilePassword() {
        return (char[]) azureClientCertificateFilePasswordTextField.getPassword();
    }
    public @Nullable char[] getAzureTokenClientSecret() {
        return azureClientSecretPasswordField.getPassword();
    }
    public @Nullable String getAzureTokenDatabaseAppIdUri() {
        return (String) azureAppIdUriTextField.getText();
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
}
