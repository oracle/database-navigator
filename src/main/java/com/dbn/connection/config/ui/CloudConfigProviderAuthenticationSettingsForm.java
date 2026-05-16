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
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Commons;
import com.dbn.connection.config.imports.CloudConfigProviderAuthentication;
import com.dbn.connection.config.imports.CloudConfigProviderType;
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
    private JPanel mainPanel;
    private JComboBox<CloudConfigProviderAuthentication> authenticationComboBox;
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
        return getSelection(authenticationComboBox);
    }

    public String getOciConfigProviderProfile() {
        return getSelection(profileComboBox);
    }

    public String getOciConfigProviderConfigFile() {
        return getText(configFileTextField);
    }

    public void applyFormChanges(DatabaseInfo databaseInfo) {
        databaseInfo.setCloudConfigProviderAuthentication(getCloudConfigProviderAuthentication());
        databaseInfo.setOciConfigProviderConfigFile(isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null);
        databaseInfo.setOciConfigProviderProfile(isDefaultAuthentication() ? getOciConfigProviderProfile() : null);
    }

    public void resetFormChanges() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        setCloudProviderType(databaseInfo.getCloudConfigProviderType());
        setSelection(authenticationComboBox, Commons.nvl(
                databaseInfo.getCloudConfigProviderAuthentication(),
                CloudConfigProviderAuthentication.getDefault(cloudProviderType)));
        configFileTextField.setText(databaseInfo.getOciConfigProviderConfigFile());
        profileComboBox
                .withValueLoader(() -> OciConfigFileUtil.getConfigProfileNames(getOciConfigProviderConfigFile()))
                .withValuePreselector(p -> Objects.equals(p, databaseInfo.getOciConfigProviderProfile()))
                .triggerLoad();
        updateFieldVisibility();
    }

    public boolean settingsChanged() {
        DatabaseInfo databaseInfo = getDatabaseInfo();
        String configFile = isDefaultAuthentication() ? getOciConfigProviderConfigFile() : null;
        String profile = isDefaultAuthentication() ? getOciConfigProviderProfile() : null;
        return !Commons.match(
                    Commons.nvl(databaseInfo.getCloudConfigProviderAuthentication(), CloudConfigProviderAuthentication.getDefault(cloudProviderType)),
                    getCloudConfigProviderAuthentication()) ||
                !Commons.match(databaseInfo.getOciConfigProviderConfigFile(), configFile) ||
                !Commons.match(databaseInfo.getOciConfigProviderProfile(), profile);
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
        boolean visible = cloudProviderType != null && cloudProviderType.isOci() && isDefaultAuthentication();
        configFileLabel.setVisible(visible);
        configFileTextField.setVisible(visible);
        profileLabel.setVisible(visible);
        profileComboBox.setVisible(visible);
    }

    private boolean isDefaultAuthentication() {
        return getCloudConfigProviderAuthentication() == CloudConfigProviderAuthentication.OCI_DEFAULT;
    }
}
