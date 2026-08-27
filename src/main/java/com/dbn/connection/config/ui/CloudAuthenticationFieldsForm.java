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
import com.dbn.connection.config.provider.impl.AzureConfigProviderHandler;
import com.dbn.connection.config.provider.impl.HashicorpConfigProviderHandler;
import com.dbn.connection.config.provider.impl.OciConfigProviderHandler;
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
    private JLabel ociConfigFileLabel;
    private TextFieldWithBrowseButton ociConfigFileTextField;
    private JLabel ociProfileLabel;
    private DBNComboBox<String> ociProfileComboBox;
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
    private JLabel hcpVaultAddressLabel;
    private JTextField hcpVaultAddressTextField;
    private JLabel hcpVaultNamespaceLabel;
    private JTextField hcpVaultNamespaceTextField;
    private JLabel hcpVaultTokenLabel;
    private JPasswordField hcpVaultTokenPasswordField;
    private JLabel hcpVaultUsernameLabel;
    private JTextField hcpVaultUsernameTextField;
    private JLabel hcpVaultPasswordLabel;
    private JPasswordField hcpVaultPasswordField;
    private JLabel hcpUserPassAuthPathLabel;
    private JTextField hcpUserPassAuthPathTextField;
    private JLabel hcpAppRoleIdLabel;
    private JTextField hcpAppRoleIdTextField;
    private JLabel hcpSecretIdLabel;
    private JPasswordField hcpSecretIdPasswordField;
    private JLabel hcpAppRoleAuthPathLabel;
    private JTextField hcpAppRoleAuthPathTextField;
    private JLabel hcpGithubTokenLabel;
    private JPasswordField hcpGithubTokenPasswordField;
    private JLabel hcpGithubAuthPathLabel;
    private JTextField hcpGithubAuthPathTextField;
    private CloudConfigProviderType cloudProviderType;

    public CloudAuthenticationFieldsForm(@NotNull ConnectionAuthenticationSettingsForm parentComponent) {
        super(parentComponent);

        ociProfileComboBox.withValueLoader(() -> loadOciConfigProfiles());

        setCloudProviderType(CloudConfigProviderType.OCI_OBJECT);

        addSingleFileChooser(
                getProject(), ociConfigFileTextField,
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
                ociProfileComboBox.reloadValues();
            }
        });
        onTextChange(ociConfigFileTextField, e -> ociProfileComboBox.reloadValues());
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
        return getSelection(ociProfileComboBox);
    }

    public String getOciConfigProviderConfigFile() {
        return getText(ociConfigFileTextField);
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
                    ociConfigFileTextField.getTextField(),
                    txt("cfg.connection.label.OciConfigFile"),
                    true);
        }
    }

    public void applyFormChanges(ConfigProviderInfo configProvider) {
        CloudAuthenticationType authentication = getCloudConfigProviderAuthentication();
        if (isOciProvider()) {
            OciConfigProviderHandler.applyAuthentication(
                    configProvider,
                    authentication,
                    getText(ociConfigFileTextField),
                    getSelection(ociProfileComboBox));
        } else if (isAzureProvider()) {
            AzureConfigProviderHandler.applyAuthentication(
                    configProvider,
                    authentication,
                    getText(azureClientIdTextField),
                    getText(azureTenantIdTextField),
                    getText(azureClientCertificatePathTextField),
                    getPassword(azureClientSecretPasswordField),
                    getPassword(azureClientCertificatePasswordField));
        } else if (isHashicorpProvider()) {
            HashicorpConfigProviderHandler.applyAuthentication(
                    configProvider,
                    authentication,
                    getText(hcpVaultAddressTextField),
                    getText(hcpVaultNamespaceTextField),
                    getText(hcpVaultUsernameTextField),
                    getText(hcpUserPassAuthPathTextField),
                    getText(hcpAppRoleIdTextField),
                    getText(hcpAppRoleAuthPathTextField),
                    getText(hcpGithubAuthPathTextField),
                    getPassword(hcpVaultTokenPasswordField),
                    getPassword(hcpVaultPasswordField),
                    getPassword(hcpSecretIdPasswordField),
                    getPassword(hcpGithubTokenPasswordField));
        }
    }

    public void resetFormChanges() {
        ConfigProviderInfo configProvider = getConfigProviderInfo();
        configProvider.reloadSecrets();
        setCloudProviderType(configProvider.getCloudProviderType());

        if (isAuthenticationProvider()) {
            setSelection(authenticationComboBox, Commons.nvl(
                    configProvider.getCloudProviderAuthentication(),
                    getDefault(cloudProviderType)));
        }

        if (isOciProvider()) {
            setText(ociConfigFileTextField, configProvider.getOciConfigFile());
            applyDefaultOciConfigFile();
            ociProfileComboBox
                    .withValuePreselector(p -> Objects.equals(p, configProvider.getOciConfigProfile()))
                    .triggerLoad();
        } else {
            setText(ociConfigFileTextField, null);
            ociProfileComboBox.removeAllItems();
        }

        boolean azureProvider = isAzureProvider();
        boolean hashicorpProvider = isHashicorpProvider();

        setText(azureClientIdTextField, azureProvider ? configProvider.getAzureClientId() : null);
        setText(azureTenantIdTextField, azureProvider ? configProvider.getAzureTenantId() : null);
        setText(azureClientCertificatePathTextField, azureProvider ? configProvider.getAzureClientCertificatePath() : null);
        setPassword(azureClientSecretPasswordField, azureProvider ? configProvider.getAzureClientSecret() : null);
        setPassword(azureClientCertificatePasswordField, azureProvider ? configProvider.getAzureClientCertificatePassword() : null);

        setText(hcpVaultAddressTextField, hashicorpProvider ? configProvider.getHashicorpVaultAddress() : null);
        setText(hcpVaultNamespaceTextField, hashicorpProvider ? configProvider.getHashicorpVaultNamespace() : null);
        setText(hcpVaultUsernameTextField, hashicorpProvider ? configProvider.getHashicorpVaultUsername() : null);
        setText(hcpUserPassAuthPathTextField, hashicorpProvider ? configProvider.getHashicorpUserpassAuthPath() : null);
        setText(hcpAppRoleIdTextField, hashicorpProvider ? configProvider.getHashicorpAppRoleId() : null);
        setText(hcpAppRoleAuthPathTextField, hashicorpProvider ? configProvider.getHashicorpAppRoleAuthPath() : null);
        setText(hcpGithubAuthPathTextField, hashicorpProvider ? configProvider.getHashicorpGithubAuthPath() : null);
        setPassword(hcpVaultTokenPasswordField, hashicorpProvider ? configProvider.getHashicorpVaultToken() : null);
        setPassword(hcpVaultPasswordField, hashicorpProvider ? configProvider.getHashicorpVaultPassword() : null);
        setPassword(hcpSecretIdPasswordField, hashicorpProvider ? configProvider.getHashicorpAppRoleSecretId() : null);
        setPassword(hcpGithubTokenPasswordField, hashicorpProvider ? configProvider.getHashicorpGithubToken() : null);
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
                    !match(configProvider.getHashicorpVaultAddress(), getText(hcpVaultAddressTextField)) ||
                    !match(configProvider.getHashicorpVaultNamespace(), getText(hcpVaultNamespaceTextField)) ||
                    isHashicorpUserpassAuthentication() && !match(configProvider.getHashicorpVaultUsername(), getText(hcpVaultUsernameTextField)) ||
                    isHashicorpUserpassAuthentication() && !match(configProvider.getHashicorpUserpassAuthPath(), getText(hcpUserPassAuthPathTextField)) ||
                    isHashicorpUserpassAuthentication() && !matchArrays(configProvider.getHashicorpVaultPassword(), getPassword(hcpVaultPasswordField)) ||
                    isHashicorpVaultTokenAuthentication() && !matchArrays(configProvider.getHashicorpVaultToken(), getPassword(hcpVaultTokenPasswordField)) ||
                    isHashicorpAppRoleAuthentication() && !match(configProvider.getHashicorpAppRoleId(), getText(hcpAppRoleIdTextField)) ||
                    isHashicorpAppRoleAuthentication() && !match(configProvider.getHashicorpAppRoleAuthPath(), getText(hcpAppRoleAuthPathTextField)) ||
                    isHashicorpAppRoleAuthentication() && !matchArrays(configProvider.getHashicorpAppRoleSecretId(), getPassword(hcpSecretIdPasswordField)) ||
                    isHashicorpGithubAuthentication() && !match(configProvider.getHashicorpGithubAuthPath(), getText(hcpGithubAuthPathTextField)) ||
                    isHashicorpGithubAuthentication() && !matchArrays(configProvider.getHashicorpGithubToken(), getPassword(hcpGithubTokenPasswordField));
        }
        if (!isOciProvider()) return authenticationChanged;

        return authenticationChanged ||
                !match(configProvider.getOciConfigFile(), configFile) ||
                !match(configProvider.getOciConfigProfile(), profile);
    }

    public void addChangeListeners(Runnable runnable) {
        authenticationComboBox.addActionListener(e -> runnable.run());
        onTextChange(ociConfigFileTextField, e -> runnable.run());
        onTextChange(azureClientIdTextField, e -> runnable.run());
        onTextChange(azureTenantIdTextField, e -> runnable.run());
        onTextChange(azureClientSecretPasswordField, e -> runnable.run());
        onTextChange(azureClientCertificatePathTextField, e -> runnable.run());
        onTextChange(azureClientCertificatePasswordField, e -> runnable.run());
        onTextChange(hcpVaultAddressTextField, e -> runnable.run());
        onTextChange(hcpVaultNamespaceTextField, e -> runnable.run());
        onTextChange(hcpVaultTokenPasswordField, e -> runnable.run());
        onTextChange(hcpVaultUsernameTextField, e -> runnable.run());
        onTextChange(hcpVaultPasswordField, e -> runnable.run());
        onTextChange(hcpUserPassAuthPathTextField, e -> runnable.run());
        onTextChange(hcpAppRoleIdTextField, e -> runnable.run());
        onTextChange(hcpSecretIdPasswordField, e -> runnable.run());
        onTextChange(hcpAppRoleAuthPathTextField, e -> runnable.run());
        onTextChange(hcpGithubTokenPasswordField, e -> runnable.run());
        onTextChange(hcpGithubAuthPathTextField, e -> runnable.run());
        ociProfileComboBox.addActionListener(e -> runnable.run());
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
        if (isOciDefaultAuthentication() && isEmpty(getText(ociConfigFileTextField))) {
            ociConfigFileTextField.setText(OciConfigUtil.getDefaultConfigFilePath());
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
        boolean hcpVaultTokenAuthentication = isHashicorpVaultTokenAuthentication();
        boolean hcpUserpassAuthentication = isHashicorpUserpassAuthentication();
        boolean hcpAppRoleAuthentication = isHashicorpAppRoleAuthentication();
        boolean hashicorpGithubAuthentication = isHashicorpGithubAuthentication();

        authenticationLabel.setVisible(authenticationProvider);
        authenticationComboBox.setVisible(authenticationProvider);
        authenticationInfoLabel.setVisible(infoProvider);
        authenticationInfoHyperlink.setVisible(infoProvider);
        ociConfigFileLabel.setVisible(ociDefaultAuthentication);
        ociConfigFileTextField.setVisible(ociDefaultAuthentication);
        ociProfileLabel.setVisible(ociDefaultAuthentication);
        ociProfileComboBox.setVisible(ociDefaultAuthentication);

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

        hcpVaultAddressLabel.setVisible(hashicorpProvider);
        hcpVaultAddressTextField.setVisible(hashicorpProvider);
        hcpVaultNamespaceLabel.setVisible(hashicorpProvider);
        hcpVaultNamespaceTextField.setVisible(hashicorpProvider);
        hcpVaultTokenLabel.setVisible(hcpVaultTokenAuthentication);
        hcpVaultTokenPasswordField.setVisible(hcpVaultTokenAuthentication);
        hcpVaultUsernameLabel.setVisible(hcpUserpassAuthentication);
        hcpVaultUsernameTextField.setVisible(hcpUserpassAuthentication);
        hcpVaultPasswordLabel.setVisible(hcpUserpassAuthentication);
        hcpVaultPasswordField.setVisible(hcpUserpassAuthentication);
        hcpUserPassAuthPathLabel.setVisible(hcpUserpassAuthentication);
        hcpUserPassAuthPathTextField.setVisible(hcpUserpassAuthentication);
        hcpAppRoleIdLabel.setVisible(hcpAppRoleAuthentication);
        hcpAppRoleIdTextField.setVisible(hcpAppRoleAuthentication);
        hcpSecretIdLabel.setVisible(hcpAppRoleAuthentication);
        hcpSecretIdPasswordField.setVisible(hcpAppRoleAuthentication);
        hcpAppRoleAuthPathLabel.setVisible(hcpAppRoleAuthentication);
        hcpAppRoleAuthPathTextField.setVisible(hcpAppRoleAuthentication);
        hcpGithubTokenLabel.setVisible(hashicorpGithubAuthentication);
        hcpGithubTokenPasswordField.setVisible(hashicorpGithubAuthentication);
        hcpGithubAuthPathLabel.setVisible(hashicorpGithubAuthentication);
        hcpGithubAuthPathTextField.setVisible(hashicorpGithubAuthentication);

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
