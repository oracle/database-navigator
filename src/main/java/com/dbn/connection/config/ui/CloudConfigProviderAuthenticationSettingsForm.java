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
    private CloudConfigProviderType cloudProviderType;

    public CloudConfigProviderAuthenticationSettingsForm(@NotNull ConnectionDatabaseSettingsForm parentComponent) {
        super(parentComponent);

        setCloudProviderType(CloudConfigProviderType.OCI_OBJECT);

        addSingleFileChooser(
                getProject(), configFileTextField,
                "Select OCI Configuration File",
                "Select the OCI config file (usually ~/.oci/config)");
        authenticationComboBox.addActionListener(e -> updateFieldVisibility());
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
        return isOciProvider() ? getSelection(authenticationComboBox) : null;
    }

    public String getOciConfigProviderProfile() {
        return getSelection(profileComboBox);
    }

    public String getOciConfigProviderConfigFile() {
        return getText(configFileTextField);
    }

    public void applyFormChanges(ConfigProviderInfo configProviderInfo) {
        configProviderInfo.applyOciAuthentication(
                getCloudConfigProviderAuthentication(),
                isOciProvider() && isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null,
                isOciProvider() && isDefaultAuthentication() ? getOciConfigProviderProfile() : null);
    }

    public void resetFormChanges() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        setCloudProviderType(databaseInfo.getConfigProviderInfo().getCloudProviderType());

        if (isOciProvider()) {
            setSelection(authenticationComboBox, Commons.nvl(
                    databaseInfo.getConfigProviderInfo().getAuthentication(),
                    CloudConfigProviderAuthentication.getDefault(cloudProviderType)));
            configFileTextField.setText(databaseInfo.getConfigProviderInfo().getOciConfigFile());
            profileComboBox
                    .withValueLoader(() -> OciConfigFileUtil.getConfigProfileNames(getOciConfigProviderConfigFile()))
                    .withValuePreselector(p -> Objects.equals(p, databaseInfo.getConfigProviderInfo().getOciProfile()))
                    .triggerLoad();
        } else {
            setSelection(authenticationComboBox, null);
            configFileTextField.setText(null);
            profileComboBox.removeAllItems();
        }
        updateFieldVisibility();
    }

    public boolean settingsChanged() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        if (!isOciProvider()) return false;

        String configFile = isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null;
        String profile = isDefaultAuthentication() ? getOciConfigProviderProfile() : null;
        return !Commons.match(
                    Commons.nvl(databaseInfo.getConfigProviderInfo().getAuthentication(), CloudConfigProviderAuthentication.getDefault(cloudProviderType)),
                    getCloudConfigProviderAuthentication()) ||
                !Commons.match(databaseInfo.getConfigProviderInfo().getOciConfigFile(), configFile) ||
                !Commons.match(databaseInfo.getConfigProviderInfo().getOciProfile(), profile);
    }

    public void addChangeListeners(Runnable runnable) {
        authenticationComboBox.addActionListener(e -> runnable.run());
        onTextChange(configFileTextField, e -> runnable.run());
        profileComboBox.addActionListener(e -> runnable.run());
    }

    private DatabaseInfo getDatabaseInfo() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        return parent.getConfiguration().getDatabaseInfo();
    }

    private void updateFieldVisibility() {
        boolean ociProvider = isOciProvider();
        boolean infoProvider = isInfoProvider();
        boolean ociDefaultAuthentication = ociProvider && isDefaultAuthentication();

        authenticationLabel.setVisible(ociProvider);
        authenticationComboBox.setVisible(ociProvider);
        authenticationInfoLabel.setVisible(infoProvider);
        authenticationInfoHyperlink.setVisible(infoProvider);
        configFileLabel.setVisible(ociDefaultAuthentication);
        configFileTextField.setVisible(ociDefaultAuthentication);
        profileLabel.setVisible(ociDefaultAuthentication);
        profileComboBox.setVisible(ociDefaultAuthentication);

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

    private boolean isOciProvider() {
        return cloudProviderType != null && cloudProviderType.isOci();
    }

    private boolean isInfoProvider() {
        return cloudProviderType != null && (cloudProviderType.isGcp() || cloudProviderType.isAws());
    }
}
