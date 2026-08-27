/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.config.ui;

import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.provider.CloudAuthenticationType;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.credentials.Secret;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.dbn.oci.config.OciConfigUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.setPassword;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Commons.match;
import static com.dbn.common.util.Commons.matchArrays;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_SECRET;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_APPROLE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_GITHUB;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_USERPASS;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_VAULT_TOKEN;
import static com.dbn.connection.config.provider.CloudAuthenticationType.OCI_DEFAULT;
import static com.dbn.connection.config.provider.CloudAuthenticationType.getDefault;
import static com.dbn.connection.config.provider.CloudAuthenticationType.values;
import static com.dbn.nls.NlsResources.txt;

public class CloudAuthenticationFieldsForm extends DBNFormBase {
    private static final String GCP_AUTHENTICATION_URL =
            "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-gcp/README.md#authentication";
    private static final String AWS_AUTHENTICATION_URL =
            "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-aws/README.md#common-parameters-for-centralized-config-providers";

    private JPanel mainPanel;
    private JLabel authenticationLabel;
    private JComboBox<CloudAuthenticationType> authenticationComboBox;
    private JLabel authenticationInfoLabel;
    private DBNHyperlinkLabel authenticationInfoHyperlink;
    private JLabel configFileLabel;
    private TextFieldWithBrowseButton configFileTextField;
    private JLabel profileLabel;
    private DBNComboBox<String> profileComboBox;
    private JLabel azureClientIdLabel;
    private JTextField azureClientIdTextField;
    private JLabel azureTenantIdLabel;
    private JTextField azureTenantIdTextField;
    private JLabel azureClientSecretLabel;
    private JPasswordField azureClientSecretPasswordField;
    private JLabel azureClientCertificatePathLabel;
    private TextFieldWithBrowseButton azureClientCertificatePathTextField;
    private JLabel azureClientCertificatePasswordLabel;
    private JPasswordField azureClientCertificatePasswordField;
    private JLabel vaultAddressLabel;
    private JTextField vaultAddressTextField;
    private JLabel vaultNamespaceLabel;
    private JTextField vaultNamespaceTextField;
    private JLabel vaultTokenLabel;
    private JPasswordField vaultTokenPasswordField;
    private JLabel vaultUsernameLabel;
    private JTextField vaultUsernameTextField;
    private JLabel vaultPasswordLabel;
    private JPasswordField vaultPasswordField;
    private JLabel userPassAuthPathLabel;
    private JTextField userPassAuthPathTextField;
    private JLabel roleIdLabel;
    private JTextField roleIdTextField;
    private JLabel secretIdLabel;
    private JPasswordField secretIdPasswordField;
    private JLabel appRoleAuthPathLabel;
    private JTextField appRoleAuthPathTextField;
    private JLabel githubTokenLabel;
    private JPasswordField githubTokenPasswordField;
    private JLabel githubAuthPathLabel;
    private JTextField githubAuthPathTextField;
    private CloudConfigProviderType cloudProviderType;

    public CloudAuthenticationFieldsForm(@NotNull ConnectionAuthenticationSettingsForm parentComponent) {
        super(parentComponent);

        profileComboBox.withValueLoader(() -> loadOciConfigProfiles());

        setCloudProviderType(CloudConfigProviderType.OCI_OBJECT);

        addSingleFileChooser(
                getProject(), configFileTextField,
                "Select OCI Configuration File",
                "Select the OCI config file (usually ~/.oci/config)");
        addSingleFileChooser(
                getProject(), azureClientCertificatePathTextField,
                "Select Azure Client Certificate",
                "Select the Azure service principal certificate file");
        authenticationComboBox.addActionListener(e -> {
            applyDefaultOciConfigFile();
            updateFieldVisibility();
            if (isOciDefaultAuthentication()) {
                profileComboBox.reloadValues();
            }
        });
        onTextChange(configFileTextField, e -> profileComboBox.reloadValues());
        updateFieldVisibility();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void setCloudProviderType(CloudConfigProviderType cloudProviderType) {
        if (this.cloudProviderType == cloudProviderType && authenticationComboBox.getItemCount() > 0) return;

        this.cloudProviderType = cloudProviderType;
        CloudAuthenticationType[] authenticationTypes = values(cloudProviderType);
        initComboBox(authenticationComboBox, authenticationTypes);
        setSelection(authenticationComboBox, getDefault(cloudProviderType));
        updateFieldVisibility();
    }

    public CloudAuthenticationType getCloudConfigProviderAuthentication() {
        return isAuthenticationProvider() ? getSelection(authenticationComboBox) : null;
    }

    public String getOciConfigProviderProfile() {
        return getSelection(profileComboBox);
    }

    public String getOciConfigProviderConfigFile() {
        return getText(configFileTextField);
    }

    public OciAuthenticationConfig getOciAuthenticationConfig() {
        return new OciAuthenticationConfig(
                getCloudConfigProviderAuthentication(),
                getOciConfigProviderConfigFile(),
                getOciConfigProviderProfile());
    }

    public void validateSettings() throws ConfigurationException {
        if (isOciDefaultAuthentication()) {
            ConfigurationEditors.validateStringValue(
                    configFileTextField.getTextField(),
                    txt("cfg.connection.label.OciConfigFile"),
                    true);
        }
    }

    public void applyFormChanges(ConfigProviderInfo configProviderInfo) {
        CloudAuthenticationType authentication = getCloudConfigProviderAuthentication();
        if (isOciProvider()) {
            configProviderInfo.applyOciAuthentication(
                    authentication,
                    isOciDefaultAuthentication() ? getOciConfigProviderConfigFile() : null,
                    isOciDefaultAuthentication() ? getOciConfigProviderProfile() : null);
        } else if (isAzureProvider()) {
            if (isAzureClientIdAuthentication()) {
                configProviderInfo.applyAzureAuthentication(
                        authentication,
                        getText(azureClientIdTextField),
                        getText(azureTenantIdTextField),
                        getText(azureClientCertificatePathTextField));
            } else {
                configProviderInfo.applyAzureAuthentication(authentication, null, null, null);
            }
            configProviderInfo.setAzureClientSecret(isAzureServicePrincipalSecretAuthentication() ?
                    getPassword(azureClientSecretPasswordField, configProviderInfo.getAzureClientSecret()) : Secret.EMPTY);
            configProviderInfo.setAzureClientCertificatePassword(isAzureServicePrincipalCertificateAuthentication() ?
                    getPassword(azureClientCertificatePasswordField, configProviderInfo.getAzureClientCertificatePassword()) : Secret.EMPTY);
        } else if (isHashicorpProvider()) {
            configProviderInfo.applyHashicorpAuthentication(
                    authentication,
                    getText(vaultAddressTextField),
                    getText(vaultNamespaceTextField),
                    getText(vaultUsernameTextField),
                    getText(userPassAuthPathTextField),
                    getText(roleIdTextField),
                    getText(appRoleAuthPathTextField),
                    getText(githubAuthPathTextField));
            configProviderInfo.setHashicorpVaultToken(isHashicorpVaultTokenAuthentication() ?
                    getPassword(vaultTokenPasswordField, configProviderInfo.getHashicorpVaultToken()) : Secret.EMPTY);
            configProviderInfo.setHashicorpVaultPassword(isHashicorpUserpassAuthentication() ?
                    getPassword(vaultPasswordField, configProviderInfo.getHashicorpVaultPassword()) : Secret.EMPTY);
            configProviderInfo.setHashicorpAppRoleSecretId(isHashicorpAppRoleAuthentication() ?
                    getPassword(secretIdPasswordField, configProviderInfo.getHashicorpAppRoleSecretId()) : Secret.EMPTY);
            configProviderInfo.setHashicorpGithubToken(isHashicorpGithubAuthentication() ?
                    getPassword(githubTokenPasswordField, configProviderInfo.getHashicorpGithubToken()) : Secret.EMPTY);
        }
    }

    public void resetFormChanges() {
        ConfigProviderInfo configProviderInfo = getConfigProviderInfo();
        configProviderInfo.reloadSecrets();
        setCloudProviderType(configProviderInfo.getCloudProviderType());

        if (isAuthenticationProvider()) {
            setSelection(authenticationComboBox, Commons.nvl(
                    configProviderInfo.getCloudProviderAuthentication(),
                    getDefault(cloudProviderType)));
        }

        if (isOciProvider()) {
            setText(configFileTextField, configProviderInfo.getOciConfigFile());
            applyDefaultOciConfigFile();
            profileComboBox
                    .withValuePreselector(p -> Objects.equals(p, configProviderInfo.getOciConfigProfile()))
                    .triggerLoad();
        } else {
            setText(configFileTextField, null);
            profileComboBox.removeAllItems();
        }

        boolean azureProvider = isAzureProvider();
        boolean hashicorpProvider = isHashicorpProvider();

        setText(azureClientIdTextField, azureProvider ? configProviderInfo.getAzureClientId() : null);
        setText(azureTenantIdTextField, azureProvider ? configProviderInfo.getAzureTenantId() : null);
        setText(azureClientCertificatePathTextField, azureProvider ? configProviderInfo.getAzureClientCertificatePath() : null);
        setPassword(azureClientSecretPasswordField, azureProvider ? configProviderInfo.getAzureClientSecret() : null);
        setPassword(azureClientCertificatePasswordField, azureProvider ? configProviderInfo.getAzureClientCertificatePassword() : null);

        setText(vaultAddressTextField, hashicorpProvider ? configProviderInfo.getHashicorpVaultAddress() : null);
        setText(vaultNamespaceTextField, hashicorpProvider ? configProviderInfo.getHashicorpVaultNamespace() : null);
        setText(vaultUsernameTextField, hashicorpProvider ? configProviderInfo.getHashicorpVaultUsername() : null);
        setText(userPassAuthPathTextField, hashicorpProvider ? configProviderInfo.getHashicorpUserpassAuthPath() : null);
        setText(roleIdTextField, hashicorpProvider ? configProviderInfo.getHashicorpAppRoleRoleId() : null);
        setText(appRoleAuthPathTextField, hashicorpProvider ? configProviderInfo.getHashicorpAppRoleAuthPath() : null);
        setText(githubAuthPathTextField, hashicorpProvider ? configProviderInfo.getHashicorpGithubAuthPath() : null);
        setPassword(vaultTokenPasswordField, hashicorpProvider ? configProviderInfo.getHashicorpVaultToken() : null);
        setPassword(vaultPasswordField, hashicorpProvider ? configProviderInfo.getHashicorpVaultPassword() : null);
        setPassword(secretIdPasswordField, hashicorpProvider ? configProviderInfo.getHashicorpAppRoleSecretId() : null);
        setPassword(githubTokenPasswordField, hashicorpProvider ? configProviderInfo.getHashicorpGithubToken() : null);
        updateFieldVisibility();
    }

    public boolean settingsChanged() {
        ConfigProviderInfo configProvider = getConfigProviderInfo();
        if (!isAuthenticationProvider()) return false;

        String configFile = isOciDefaultAuthentication() ? getOciConfigProviderConfigFile() : null;
        String profile = isOciDefaultAuthentication() ? getOciConfigProviderProfile() : null;
        boolean authenticationChanged = !match(
                Commons.nvl(configProvider.getCloudProviderAuthentication(), getDefault(cloudProviderType)),
                getCloudConfigProviderAuthentication());
        if (isAzureProvider()) {
            return authenticationChanged ||
                    isAzureClientIdAuthentication() && !match(configProvider.getAzureClientId(), getText(azureClientIdTextField)) ||
                    isAzureServicePrincipalAuthentication() && !match(configProvider.getAzureTenantId(), getText(azureTenantIdTextField)) ||
                    isAzureServicePrincipalSecretAuthentication() && !matchArrays(configProvider.getAzureClientSecret(), getPassword(azureClientSecretPasswordField)) ||
                    isAzureServicePrincipalCertificateAuthentication() && !match(configProvider.getAzureClientCertificatePath(), getText(azureClientCertificatePathTextField)) ||
                    isAzureServicePrincipalCertificateAuthentication() && !matchArrays(configProvider.getAzureClientCertificatePassword(), getPassword(azureClientCertificatePasswordField));
        }
        if (isHashicorpProvider()) {
            return authenticationChanged ||
                    !match(configProvider.getHashicorpVaultAddress(), getText(vaultAddressTextField)) ||
                    !match(configProvider.getHashicorpVaultNamespace(), getText(vaultNamespaceTextField)) ||
                    isHashicorpUserpassAuthentication() && !match(configProvider.getHashicorpVaultUsername(), getText(vaultUsernameTextField)) ||
                    isHashicorpUserpassAuthentication() && !match(configProvider.getHashicorpUserpassAuthPath(), getText(userPassAuthPathTextField)) ||
                    isHashicorpUserpassAuthentication() && !matchArrays(configProvider.getHashicorpVaultPassword(), getPassword(vaultPasswordField)) ||
                    isHashicorpVaultTokenAuthentication() && !matchArrays(configProvider.getHashicorpVaultToken(), getPassword(vaultTokenPasswordField)) ||
                    isHashicorpAppRoleAuthentication() && !match(configProvider.getHashicorpAppRoleRoleId(), getText(roleIdTextField)) ||
                    isHashicorpAppRoleAuthentication() && !match(configProvider.getHashicorpAppRoleAuthPath(), getText(appRoleAuthPathTextField)) ||
                    isHashicorpAppRoleAuthentication() && !matchArrays(configProvider.getHashicorpAppRoleSecretId(), getPassword(secretIdPasswordField)) ||
                    isHashicorpGithubAuthentication() && !match(configProvider.getHashicorpGithubAuthPath(), getText(githubAuthPathTextField)) ||
                    isHashicorpGithubAuthentication() && !matchArrays(configProvider.getHashicorpGithubToken(), getPassword(githubTokenPasswordField));
        }
        if (!isOciProvider()) return authenticationChanged;

        return authenticationChanged ||
                !match(configProvider.getOciConfigFile(), configFile) ||
                !match(configProvider.getOciConfigProfile(), profile);
    }

    public void addChangeListeners(Runnable runnable) {
        authenticationComboBox.addActionListener(e -> runnable.run());
        onTextChange(configFileTextField, e -> runnable.run());
        onTextChange(azureClientIdTextField, e -> runnable.run());
        onTextChange(azureTenantIdTextField, e -> runnable.run());
        onTextChange(azureClientSecretPasswordField, e -> runnable.run());
        onTextChange(azureClientCertificatePathTextField, e -> runnable.run());
        onTextChange(azureClientCertificatePasswordField, e -> runnable.run());
        onTextChange(vaultAddressTextField, e -> runnable.run());
        onTextChange(vaultNamespaceTextField, e -> runnable.run());
        onTextChange(vaultTokenPasswordField, e -> runnable.run());
        onTextChange(vaultUsernameTextField, e -> runnable.run());
        onTextChange(vaultPasswordField, e -> runnable.run());
        onTextChange(userPassAuthPathTextField, e -> runnable.run());
        onTextChange(roleIdTextField, e -> runnable.run());
        onTextChange(secretIdPasswordField, e -> runnable.run());
        onTextChange(appRoleAuthPathTextField, e -> runnable.run());
        onTextChange(githubTokenPasswordField, e -> runnable.run());
        onTextChange(githubAuthPathTextField, e -> runnable.run());
        profileComboBox.addActionListener(e -> runnable.run());
    }

    private ConfigProviderInfo getConfigProviderInfo() {
        return getDatabaseSettings().getConfigProviderInfo();
    }

    public ConnectionId getConnectionId() {
        return getDatabaseSettings().getConnectionId();
    }

    private ConnectionDatabaseSettings getDatabaseSettings() {
        ConnectionDatabaseSettingsForm parent = ensureParentFrom(ConnectionDatabaseSettingsForm.class);
        return parent.getConfiguration();
    }

    private List<String> loadOciConfigProfiles() {
        return OciConfigUtil.getConfigProfileNames(getOciConfigProviderConfigFile());
    }

    private void applyDefaultOciConfigFile() {
        if (isOciDefaultAuthentication() && isEmpty(getText(configFileTextField))) {
            configFileTextField.setText(OciConfigUtil.getDefaultConfigFilePath());
        }
    }

    private void updateFieldVisibility() {
        boolean authenticationProvider = isAuthenticationProvider();
        boolean infoProvider = isInfoProvider();
        boolean ociDefaultAuthentication = isOciDefaultAuthentication();
        boolean azureInteractiveAuthentication = isAzureInteractiveAuthentication();
        boolean azureClientIdAuthentication = isAzureClientIdAuthentication();
        boolean azureServicePrincipalAuthentication = isAzureServicePrincipalAuthentication();
        boolean azureServicePrincipalSecretAuthentication = isAzureServicePrincipalSecretAuthentication();
        boolean azureServicePrincipalCertificateAuthentication = isAzureServicePrincipalCertificateAuthentication();
        boolean hashicorpProvider = isHashicorpProvider();
        boolean hashicorpVaultTokenAuthentication = isHashicorpVaultTokenAuthentication();
        boolean hashicorpUserpassAuthentication = isHashicorpUserpassAuthentication();
        boolean hashicorpAppRoleAuthentication = isHashicorpAppRoleAuthentication();
        boolean hashicorpGithubAuthentication = isHashicorpGithubAuthentication();

        authenticationLabel.setVisible(authenticationProvider);
        authenticationComboBox.setVisible(authenticationProvider);
        authenticationInfoLabel.setVisible(infoProvider);
        authenticationInfoHyperlink.setVisible(infoProvider);
        configFileLabel.setVisible(ociDefaultAuthentication);
        configFileTextField.setVisible(ociDefaultAuthentication);
        profileLabel.setVisible(ociDefaultAuthentication);
        profileComboBox.setVisible(ociDefaultAuthentication);
        azureClientIdLabel.setVisible(azureClientIdAuthentication);
        azureClientIdTextField.setVisible(azureClientIdAuthentication);
        azureTenantIdLabel.setVisible(azureServicePrincipalAuthentication);
        azureTenantIdTextField.setVisible(azureServicePrincipalAuthentication);
        azureClientSecretLabel.setVisible(azureServicePrincipalSecretAuthentication);
        azureClientSecretPasswordField.setVisible(azureServicePrincipalSecretAuthentication);
        azureClientCertificatePathLabel.setVisible(azureServicePrincipalCertificateAuthentication);
        azureClientCertificatePathTextField.setVisible(azureServicePrincipalCertificateAuthentication);
        azureClientCertificatePasswordLabel.setVisible(azureServicePrincipalCertificateAuthentication);
        azureClientCertificatePasswordField.setVisible(azureServicePrincipalCertificateAuthentication);
        vaultAddressLabel.setVisible(hashicorpProvider);
        vaultAddressTextField.setVisible(hashicorpProvider);
        vaultNamespaceLabel.setVisible(hashicorpProvider);
        vaultNamespaceTextField.setVisible(hashicorpProvider);
        vaultTokenLabel.setVisible(hashicorpVaultTokenAuthentication);
        vaultTokenPasswordField.setVisible(hashicorpVaultTokenAuthentication);
        vaultUsernameLabel.setVisible(hashicorpUserpassAuthentication);
        vaultUsernameTextField.setVisible(hashicorpUserpassAuthentication);
        vaultPasswordLabel.setVisible(hashicorpUserpassAuthentication);
        vaultPasswordField.setVisible(hashicorpUserpassAuthentication);
        userPassAuthPathLabel.setVisible(hashicorpUserpassAuthentication);
        userPassAuthPathTextField.setVisible(hashicorpUserpassAuthentication);
        roleIdLabel.setVisible(hashicorpAppRoleAuthentication);
        roleIdTextField.setVisible(hashicorpAppRoleAuthentication);
        secretIdLabel.setVisible(hashicorpAppRoleAuthentication);
        secretIdPasswordField.setVisible(hashicorpAppRoleAuthentication);
        appRoleAuthPathLabel.setVisible(hashicorpAppRoleAuthentication);
        appRoleAuthPathTextField.setVisible(hashicorpAppRoleAuthentication);
        githubTokenLabel.setVisible(hashicorpGithubAuthentication);
        githubTokenPasswordField.setVisible(hashicorpGithubAuthentication);
        githubAuthPathLabel.setVisible(hashicorpGithubAuthentication);
        githubAuthPathTextField.setVisible(hashicorpGithubAuthentication);

        if (cloudProviderType == null) return;

        if (cloudProviderType.isGcp()) {
            authenticationInfoLabel.setText("Authentication uses Google Application Default Credentials.");
            authenticationInfoHyperlink.setHyperlinkText("Authentication details");
            authenticationInfoHyperlink.setHyperlinkTarget(GCP_AUTHENTICATION_URL);
        } else if (cloudProviderType.isAws()) {
            authenticationInfoLabel.setText("Authentication uses the AWS default credentials provider chain.");
            authenticationInfoHyperlink.setHyperlinkText("Authentication details");
            authenticationInfoHyperlink.setHyperlinkTarget(AWS_AUTHENTICATION_URL);
        }
    }

    private boolean isOciDefaultAuthentication() {
        return getCloudConfigProviderAuthentication() == OCI_DEFAULT;
    }

    private boolean isAzureInteractiveAuthentication() {
        return isAzureProvider() && getCloudConfigProviderAuthentication() == AZURE_INTERACTIVE;
    }

    private boolean isAzureClientIdAuthentication() {
        return isAzureInteractiveAuthentication() ||
                isAzureServicePrincipalAuthentication();
    }

    private boolean isAzureServicePrincipalAuthentication() {
        return isAzureServicePrincipalSecretAuthentication() ||
                isAzureServicePrincipalCertificateAuthentication();
    }

    private boolean isAzureServicePrincipalSecretAuthentication() {
        return isAzureProvider() && getCloudConfigProviderAuthentication() == AZURE_SERVICE_PRINCIPAL_SECRET;
    }

    private boolean isAzureServicePrincipalCertificateAuthentication() {
        return isAzureProvider() && getCloudConfigProviderAuthentication() == AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
    }

    private boolean isHashicorpVaultTokenAuthentication() {
        return isHashicorpProvider() && getCloudConfigProviderAuthentication() == HCP_VAULT_TOKEN;
    }

    private boolean isHashicorpUserpassAuthentication() {
        return isHashicorpProvider() && getCloudConfigProviderAuthentication() == HCP_USERPASS;
    }

    private boolean isHashicorpAppRoleAuthentication() {
        return isHashicorpProvider() && getCloudConfigProviderAuthentication() == HCP_APPROLE;
    }

    private boolean isHashicorpGithubAuthentication() {
        return isHashicorpProvider() && getCloudConfigProviderAuthentication() == HCP_GITHUB;
    }

    private boolean isOciProvider() {
        return cloudProviderType != null && cloudProviderType.isOci();
    }

    private boolean isAzureProvider() {
        return cloudProviderType != null && cloudProviderType.isAzure();
    }

    private boolean isHashicorpProvider() {
        return cloudProviderType != null && cloudProviderType.isHashicorp();
    }

    private boolean isAuthenticationProvider() {
        return cloudProviderType != null &&
                values(cloudProviderType).length > 0;
    }

    private boolean isInfoProvider() {
        return cloudProviderType != null && (cloudProviderType.isGcp() || cloudProviderType.isAws());
    }
}
