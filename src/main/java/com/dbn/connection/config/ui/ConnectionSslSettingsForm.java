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

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.connection.config.ConnectionSslSettings;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.event.ActionListener;

public class ConnectionSslSettingsForm extends ConfigurationEditorForm<ConnectionSslSettings> {
    private JPanel mainPanel;
    private JPanel sshGroupPanel;
    private JCheckBox activeCheckBox;
    private TextFieldWithBrowseButton certificateAuthFileField;
    private TextFieldWithBrowseButton clientCertificateFile;
    private TextFieldWithBrowseButton clientKeyFile;

    public ConnectionSslSettingsForm(final ConnectionSslSettings configuration) {
        super(configuration);

        resetFormChanges();

        enableDisableFields();
        registerComponent(mainPanel);

        certificateAuthFileField.addBrowseFolderListener(txt("cfg.connection.title.SelectCertificateAuthorityFile"), "",
                null, new FileChooserDescriptor(true, false, false, false, false, false));

        clientCertificateFile.addBrowseFolderListener(txt("cfg.connection.title.SelectClientCertificateFile"), "",
                null, new FileChooserDescriptor(true, false, false, false, false, false));

        clientKeyFile.addBrowseFolderListener(txt("cfg.connection.title.SelectClientKeyFile"), "",
                null, new FileChooserDescriptor(true, false, false, false, false, false));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            getConfiguration().setModified(true);
            Object source = e.getSource();

            if (source == activeCheckBox) {
                enableDisableFields();
            }
        };
    }

    private void enableDisableFields() {
        boolean enabled = activeCheckBox.isSelected();
        certificateAuthFileField.setEnabled(enabled);
        clientCertificateFile.setEnabled(enabled);
        clientKeyFile.setEnabled(enabled);

    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionSslSettings configuration = getConfiguration();
        applyFormChanges(configuration);
    }

    @Override
    public void applyFormChanges(ConnectionSslSettings configuration) throws ConfigurationException {
        boolean enabled = activeCheckBox.isSelected();
        configuration.setActive(enabled);
        configuration.setCertificateAuthorityFile(certificateAuthFileField.getText());
        configuration.setClientCertificateFile(clientCertificateFile.getText());
        configuration.setClientKeyFile(clientKeyFile.getText());
    }

    @Override
    public void resetFormChanges() {
        ConnectionSslSettings configuration = getConfiguration();
        activeCheckBox.setSelected(configuration.isActive());
        certificateAuthFileField.setText(configuration.getCertificateAuthorityFile());
        clientCertificateFile.setText(configuration.getClientCertificateFile());
        clientKeyFile.setText(configuration.getClientKeyFile());
    }

}
