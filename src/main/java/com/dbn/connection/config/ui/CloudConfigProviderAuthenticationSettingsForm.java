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
import com.dbn.common.util.Commons;
import com.dbn.connection.config.provider.CloudConfigProviderAuthentication;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.oci.config.OciConfigFileUtil;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
            if (isDefaultAuthentication()) {
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
                    isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null,
                    isDefaultAuthentication() ? getOciConfigProviderProfile() : null);
        } else if (isAzureProvider()) {
            if (isAzureInteractiveAuthentication()) {
                configProviderInfo.applyAzureAuthentication(authentication, getText(azureClientIdTextField));
            } else {
                configProviderInfo.applyAzureAuthentication(authentication, null);
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
        updateFieldVisibility();
    }

    public boolean settingsChanged() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        if (!isAuthenticationProvider()) return false;

        String configFile = isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null;
        String profile = isDefaultAuthentication() ? getOciConfigProviderProfile() : null;
        boolean authenticationChanged = !Commons.match(
                Commons.nvl(databaseInfo.getConfigProviderInfo().getAuthentication(), CloudConfigProviderAuthentication.getDefault(cloudProviderType)),
                getCloudConfigProviderAuthentication());
        if (isAzureProvider()) {
            return authenticationChanged ||
                    isAzureInteractiveAuthentication() &&
                            !Commons.match(databaseInfo.getConfigProviderInfo().getAzureClientId(), getText(azureClientIdTextField));
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
        profileComboBox.addActionListener(e -> runnable.run());
    }

    private DatabaseInfo getDatabaseInfo() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration().getDatabaseInfo();
    }

    private List<String> loadOciConfigProfiles() {
        return OciConfigFileUtil.getConfigProfileNames(getOciConfigProviderConfigFile());
    }

    private void updateFieldVisibility() {
        boolean ociProvider = isOciProvider();
        boolean authenticationProvider = isAuthenticationProvider();
        boolean infoProvider = isInfoProvider();
        boolean ociDefaultAuthentication = ociProvider && isDefaultAuthentication();
        boolean azureInteractiveAuthentication = isAzureInteractiveAuthentication();

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

    private boolean isDefaultAuthentication() {
        return getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.OCI_DEFAULT;
    }

    private boolean isAzureInteractiveAuthentication() {
        return isAzureProvider() &&
                getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.AZURE_INTERACTIVE;
    }

    private boolean isOciProvider() {
        return cloudProviderType != null && cloudProviderType.isOci();
    }

    private boolean isAzureProvider() {
        return cloudProviderType != null && cloudProviderType.isAzure();
    }

    private boolean isAuthenticationProvider() {
        return cloudProviderType != null &&
                CloudConfigProviderAuthentication.values(cloudProviderType).length > 0;
    }

    private boolean isInfoProvider() {
        return cloudProviderType != null && (cloudProviderType.isGcp() || cloudProviderType.isAws());
    }
}
