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

import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.provider.CloudConfigProviderAuthentication;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigProviderSecretStore;
import com.dbn.oci.config.OciConfigFileUtil;
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
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;

public class CloudConfigProviderAuthenticationSettingsForm extends DBNFormBase {
    private static final String GCP_AUTHENTICATION_URL =
            "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-gcp/README.md#authentication";
    private static final String AWS_AUTHENTICATION_URL =
            "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-aws/README.md#common-parameters-for-centralized-config-providers";

    private JPanel mainPanel;
    private JLabel authenticationLabel;
    private JComboBox<CloudConfigProviderAuthentication> authenticationComboBox;
    private JLabel authenticationInfoLabel;
    private DBNHyperlinkLabel authenticationInfoHyperlink;
    private JLabel configFileLabel;
    private TextFieldWithBrowseButton configFileTextField;
    private JLabel profileLabel;
    private DBNComboBox<String> profileComboBox;
    private JLabel azureClientIdLabel;
    private JTextField azureClientIdTextField;
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
    private CloudConfigProviderType cloudProviderType;

    public CloudConfigProviderAuthenticationSettingsForm(@NotNull ConnectionDatabaseSettingsForm parentComponent) {
        super(parentComponent);

        profileComboBox.withValueLoader(() -> loadOciConfigProfiles());

        setCloudProviderType(CloudConfigProviderType.OCI_OBJECT);

        addSingleFileChooser(
                getProject(), configFileTextField,
                "Select OCI Configuration File",
                "Select the OCI config file (usually ~/.oci/config)");
        authenticationComboBox.addActionListener(e -> {
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
        CloudConfigProviderAuthentication[] authenticationTypes = CloudConfigProviderAuthentication.values(cloudProviderType);
        initComboBox(authenticationComboBox, authenticationTypes);
        setSelection(authenticationComboBox, CloudConfigProviderAuthentication.getDefault(cloudProviderType));
        updateFieldVisibility();
    }

    public CloudConfigProviderAuthentication getCloudConfigProviderAuthentication() {
        return isAuthenticationProvider() ? getSelection(authenticationComboBox) : null;
    }

    public String getOciConfigProviderProfile() {
        return getSelection(profileComboBox);
    }

    public String getOciConfigProviderConfigFile() {
        return getText(configFileTextField);
    }

    public void applyFormChanges(ConfigProviderInfo configProviderInfo) {
        CloudConfigProviderAuthentication authentication = getCloudConfigProviderAuthentication();
        if (isOciProvider()) {
            configProviderInfo.applyOciAuthentication(
                    authentication,
                    isOciDefaultAuthentication() ? getOciConfigProviderConfigFile() : null,
                    isOciDefaultAuthentication() ? getOciConfigProviderProfile() : null);
        } else if (isAzureProvider()) {
            if (isAzureInteractiveAuthentication()) {
                configProviderInfo.applyAzureAuthentication(authentication, getText(azureClientIdTextField));
            } else {
                configProviderInfo.applyAzureAuthentication(authentication, null);
            }
        } else if (isHashicorpProvider()) {
            configProviderInfo.applyHashicorpAuthentication(
                    authentication,
                    getText(vaultAddressTextField),
                    getText(vaultNamespaceTextField),
                    getText(vaultUsernameTextField),
                    getText(userPassAuthPathTextField),
                    getText(roleIdTextField));
            if (isHashicorpVaultTokenAuthentication()) {
                ConfigProviderSecretStore.saveHashicorpVaultToken(getConnectionId(), vaultTokenPasswordField.getPassword());
            } else {
                ConfigProviderSecretStore.removeHashicorpVaultToken(getConnectionId());
            }
            if (isHashicorpUserpassAuthentication()) {
                ConfigProviderSecretStore.saveHashicorpVaultPassword(getConnectionId(), vaultPasswordField.getPassword());
            } else {
                ConfigProviderSecretStore.removeHashicorpVaultPassword(getConnectionId());
            }
            if (isHashicorpAppRoleAuthentication()) {
                ConfigProviderSecretStore.saveHashicorpAppRoleSecretId(getConnectionId(), secretIdPasswordField.getPassword());
            } else {
                ConfigProviderSecretStore.removeHashicorpAppRoleSecretId(getConnectionId());
            }
        }
    }

    public void resetFormChanges() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        setCloudProviderType(databaseInfo.getConfigProviderInfo().getCloudProviderType());

        if (isAuthenticationProvider()) {
            setSelection(authenticationComboBox, Commons.nvl(
                    databaseInfo.getConfigProviderInfo().getAuthentication(),
                    CloudConfigProviderAuthentication.getDefault(cloudProviderType)));
        }

        if (isOciProvider()) {
            configFileTextField.setText(databaseInfo.getConfigProviderInfo().getOciConfigFile());
            profileComboBox
                    .withValuePreselector(p -> Objects.equals(p, databaseInfo.getConfigProviderInfo().getOciProfile()))
                    .triggerLoad();
        } else {
            configFileTextField.setText(null);
            profileComboBox.removeAllItems();
        }

        azureClientIdTextField.setText(isAzureProvider() ?
                databaseInfo.getConfigProviderInfo().getAzureClientId() : null);
        vaultAddressTextField.setText(isHashicorpProvider() ?
                databaseInfo.getConfigProviderInfo().getVaultAddress() : null);
        vaultNamespaceTextField.setText(isHashicorpProvider() ?
                databaseInfo.getConfigProviderInfo().getVaultNamespace() : null);
        vaultUsernameTextField.setText(isHashicorpProvider() ?
                databaseInfo.getConfigProviderInfo().getVaultUsername() : null);
        userPassAuthPathTextField.setText(isHashicorpProvider() ?
                databaseInfo.getConfigProviderInfo().getUserPassAuthPath() : null);
        roleIdTextField.setText(isHashicorpProvider() ?
                databaseInfo.getConfigProviderInfo().getRoleId() : null);
        vaultTokenPasswordField.setText(isHashicorpProvider() ?
                Chars.toString(ConfigProviderSecretStore.loadHashicorpVaultToken(getConnectionId())) : null);
        vaultPasswordField.setText(isHashicorpProvider() ?
                Chars.toString(ConfigProviderSecretStore.loadHashicorpVaultPassword(getConnectionId())) : null);
        secretIdPasswordField.setText(isHashicorpProvider() ?
                Chars.toString(ConfigProviderSecretStore.loadHashicorpAppRoleSecretId(getConnectionId())) : null);
        updateFieldVisibility();
    }

    public boolean settingsChanged() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        if (!isAuthenticationProvider()) return false;

        String configFile = isOciDefaultAuthentication() ? getOciConfigProviderConfigFile() : null;
        String profile = isOciDefaultAuthentication() ? getOciConfigProviderProfile() : null;
        boolean authenticationChanged = !Commons.match(
                Commons.nvl(databaseInfo.getConfigProviderInfo().getAuthentication(), CloudConfigProviderAuthentication.getDefault(cloudProviderType)),
                getCloudConfigProviderAuthentication());
        if (isAzureProvider()) {
            return authenticationChanged ||
                    isAzureInteractiveAuthentication() &&
                            !Commons.match(databaseInfo.getConfigProviderInfo().getAzureClientId(), getText(azureClientIdTextField));
        }
        if (isHashicorpProvider()) {
            return authenticationChanged ||
                    !Commons.match(databaseInfo.getConfigProviderInfo().getVaultAddress(), getText(vaultAddressTextField)) ||
                    !Commons.match(databaseInfo.getConfigProviderInfo().getVaultNamespace(), getText(vaultNamespaceTextField)) ||
                    isHashicorpUserpassAuthentication() &&
                            !Commons.match(databaseInfo.getConfigProviderInfo().getVaultUsername(), getText(vaultUsernameTextField)) ||
                    isHashicorpUserpassAuthentication() &&
                            !Commons.match(databaseInfo.getConfigProviderInfo().getUserPassAuthPath(), getText(userPassAuthPathTextField)) ||
                    isHashicorpUserpassAuthentication() &&
                            !Commons.matchArrays(ConfigProviderSecretStore.loadHashicorpVaultPassword(getConnectionId()), vaultPasswordField.getPassword()) ||
                    isHashicorpVaultTokenAuthentication() &&
                            !Commons.matchArrays(ConfigProviderSecretStore.loadHashicorpVaultToken(getConnectionId()), vaultTokenPasswordField.getPassword()) ||
                    isHashicorpAppRoleAuthentication() &&
                            !Commons.match(databaseInfo.getConfigProviderInfo().getRoleId(), getText(roleIdTextField)) ||
                    isHashicorpAppRoleAuthentication() &&
                            !Commons.matchArrays(ConfigProviderSecretStore.loadHashicorpAppRoleSecretId(getConnectionId()), secretIdPasswordField.getPassword());
        }
        if (!isOciProvider()) return authenticationChanged;

        return authenticationChanged ||
                !Commons.match(databaseInfo.getConfigProviderInfo().getOciConfigFile(), configFile) ||
                !Commons.match(databaseInfo.getConfigProviderInfo().getOciProfile(), profile);
    }

    public void addChangeListeners(Runnable runnable) {
        authenticationComboBox.addActionListener(e -> runnable.run());
        onTextChange(configFileTextField, e -> runnable.run());
        onTextChange(azureClientIdTextField, e -> runnable.run());
        onTextChange(vaultAddressTextField, e -> runnable.run());
        onTextChange(vaultNamespaceTextField, e -> runnable.run());
        onTextChange(vaultTokenPasswordField, e -> runnable.run());
        onTextChange(vaultUsernameTextField, e -> runnable.run());
        onTextChange(vaultPasswordField, e -> runnable.run());
        onTextChange(userPassAuthPathTextField, e -> runnable.run());
        onTextChange(roleIdTextField, e -> runnable.run());
        onTextChange(secretIdPasswordField, e -> runnable.run());
        profileComboBox.addActionListener(e -> runnable.run());
    }

    private DatabaseInfo getDatabaseInfo() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration().getDatabaseInfo();
    }

    private ConnectionId getConnectionId() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration().getConnectionId();
    }

    private List<String> loadOciConfigProfiles() {
        return OciConfigFileUtil.getConfigProfileNames(getOciConfigProviderConfigFile());
    }

    private void updateFieldVisibility() {
        boolean authenticationProvider = isAuthenticationProvider();
        boolean infoProvider = isInfoProvider();
        boolean ociDefaultAuthentication = isOciDefaultAuthentication();
        boolean azureInteractiveAuthentication = isAzureInteractiveAuthentication();
        boolean hashicorpProvider = isHashicorpProvider();
        boolean hashicorpVaultTokenAuthentication = isHashicorpVaultTokenAuthentication();
        boolean hashicorpUserpassAuthentication = isHashicorpUserpassAuthentication();
        boolean hashicorpAppRoleAuthentication = isHashicorpAppRoleAuthentication();

        authenticationLabel.setVisible(authenticationProvider);
        authenticationComboBox.setVisible(authenticationProvider);
        authenticationInfoLabel.setVisible(infoProvider);
        authenticationInfoHyperlink.setVisible(infoProvider);
        configFileLabel.setVisible(ociDefaultAuthentication);
        configFileTextField.setVisible(ociDefaultAuthentication);
        profileLabel.setVisible(ociDefaultAuthentication);
        profileComboBox.setVisible(ociDefaultAuthentication);
        azureClientIdLabel.setVisible(azureInteractiveAuthentication);
        azureClientIdTextField.setVisible(azureInteractiveAuthentication);
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
        return getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.OCI_DEFAULT;
    }

    private boolean isAzureInteractiveAuthentication() {
        return isAzureProvider() &&
                getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.AZURE_INTERACTIVE;
    }

    private boolean isHashicorpVaultTokenAuthentication() {
        return isHashicorpProvider() &&
                getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.HCP_VAULT_TOKEN;
    }

    private boolean isHashicorpUserpassAuthentication() {
        return isHashicorpProvider() &&
                getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.HCP_USERPASS;
    }

    private boolean isHashicorpAppRoleAuthentication() {
        return isHashicorpProvider() &&
                getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.HCP_APPROLE;
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
                CloudConfigProviderAuthentication.values(cloudProviderType).length > 0;
    }

    private boolean isInfoProvider() {
        return cloudProviderType != null && (cloudProviderType.isGcp() || cloudProviderType.isAws());
    }
}
