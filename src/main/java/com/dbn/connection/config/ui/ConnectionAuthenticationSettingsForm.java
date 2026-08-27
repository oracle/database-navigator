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

package com.dbn.connection.config.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.ui.ConnectionAuthenticationFieldsForm;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/**
 * Wrapper for the {@link ConnectionAuthenticationFieldsForm} to be used in connection settings
 */
public class ConnectionAuthenticationSettingsForm extends DBNFormBase {
    private JPanel mainPanel;

    private final ConnectionAuthenticationFieldsForm connectionFieldsForm = new ConnectionAuthenticationFieldsForm(this);
    private final CloudAuthenticationFieldsForm cloudFieldsForm = new CloudAuthenticationFieldsForm(this);

    public ConnectionAuthenticationSettingsForm(@NotNull ConnectionDatabaseSettingsForm parentComponent) {
        super(parentComponent);

        setCloudProviderMode(null);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public boolean settingsChanged() {
        AuthenticationInfo authenticationInfo = getAuthenticationInfo();
        return connectionFieldsForm.settingsChanged(authenticationInfo);
    }

    public boolean cloudProviderSettingsChanged() {
        return cloudFieldsForm.settingsChanged();
    }

    public void resetFormChanges() {
        AuthenticationInfo authenticationInfo = getAuthenticationInfo();
        connectionFieldsForm.resetFormChanges(authenticationInfo);
        cloudFieldsForm.resetFormChanges();
    }

    public void setAuthenticationTypes(AuthenticationType ... authenticationTypes) {
        connectionFieldsForm.setAuthenticationTypes(authenticationTypes);
    }

    public void setCredentialsTitle(@Nls String title) {
        Border border = mainPanel.getBorder();
        if (border instanceof TitledBorder titledBorder) {
            titledBorder.setTitle(title);
        } else if (border instanceof CompoundBorder compoundBorder &&
                compoundBorder.getInsideBorder() instanceof TitledBorder titledBorder) {
            titledBorder.setTitle(title);
        }
        mainPanel.repaint();
    }

    public void setCloudProviderMode(CloudConfigProviderType cloudProviderType) {
        mainPanel.removeAll();
        if (cloudProviderType != null) {
            cloudFieldsForm.setCloudProviderType(cloudProviderType);
        }
        JComponent component = cloudProviderType != null ?
                cloudFieldsForm.getComponent() :
                connectionFieldsForm.getComponent();
        mainPanel.add(component);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void applyCloudProviderFormChanges(ConfigProviderInfo configProviderInfo) {
        cloudFieldsForm.applyFormChanges(configProviderInfo);
    }

    public void validateCloudProviderSettings() throws ConfigurationException {
        cloudFieldsForm.validateSettings();
    }

    public void addCloudProviderChangeListeners(Runnable runnable) {
        cloudFieldsForm.addChangeListeners(runnable);
    }

    public OciAuthenticationConfig getOciAuthenticationConfig() {
        return cloudFieldsForm.getOciAuthenticationConfig();
    }

    private AuthenticationInfo getAuthenticationInfo() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        ConnectionDatabaseSettings configuration = parent.getConfiguration();
        return configuration.getAuthenticationInfo();
    }

    public void applyFormChanges(AuthenticationInfo authenticationInfo) {
        connectionFieldsForm.applyFormChanges(authenticationInfo);
    }
}
