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

package com.dbn.assistant.service.selectai.ui;

import com.dbn.assistant.service.selectai.credential.ui.CredentialManagementForm;
import com.dbn.assistant.service.selectai.profile.ui.ProfileManagementForm;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;

public class ProfilesAndCredentialsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTabbedPane settingsTabbedPane;
    private JPanel credentialsPanel;
    private JPanel profilesPanel;

    private final ConnectionRef connection;

    private ProfileManagementForm profileManagementForm;
    private CredentialManagementForm credentialManagementForm;

    public ProfilesAndCredentialsForm(@Nullable DBNDialog<?> parent, ConnectionHandler connection) {
        super(parent);
        this.connection = ConnectionRef.of(connection);

        initHeaderPanel();
        initConfigTabs();
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initConfigTabs() {
        ConnectionHandler connection = getConnection();
        profileManagementForm = new ProfileManagementForm(this, connection);
        credentialManagementForm = new CredentialManagementForm(this, connection);

        profilesPanel.add(profileManagementForm.getComponent());
        credentialsPanel.add(credentialManagementForm.getComponent());
    }

    @Nullable
    protected DBNForm getSelectedConfigForm() {
        Component selectedComponent = settingsTabbedPane.getSelectedComponent();
        if (selectedComponent == profilesPanel) return profileManagementForm;
        if (selectedComponent == credentialsPanel) return credentialManagementForm;
        return null;
    }

    private ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
