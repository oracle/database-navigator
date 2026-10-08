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

import com.dbn.common.Result;
import com.dbn.common.color.Colors;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.ui.panel.DBNAsyncOperationPanel;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dbn.connection.ssh.SshAuthType;
import com.dbn.connection.ssh.SshConnections;
import com.dbn.connection.ssh.SshTunnelConfig;
import com.dbn.credentials.Secret;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.message.MessageType.ERROR;
import static com.dbn.common.message.MessageType.SUCCESS;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.setPassword;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.connection.ssh.SshConnections.createTunnelConfig;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionSshTunnelSettingsForm extends ConfigurationEditorForm<ConnectionSshTunnelSettings> {
    private JPanel mainPanel;
    private JPanel sshGroupPanel;
    private JTextField hostTextField;
    private JTextField userTextField;
    private JPasswordField passwordField;
    private JTextField portTextField;
    private JCheckBox activeCheckBox;
    private JComboBox<SshAuthType> authTypeComboBox;
    private JPasswordField keyPassphraseField;
    private TextFieldWithBrowseButton keyFileField;
    private JLabel passwordLabel;
    private JLabel privateKeyFileLabel;
    private JLabel privateKeyPassphraseLabel;
    private JPanel testTunnelConnectionPanel;

    private final DBNAsyncOperationPanel testTunnelConnectionControl = new DBNAsyncOperationPanel(
            txt("cfg.connection.link.TestTunnelConnection"),
            txt("prc.connection.title.TestingTunnelConnection"));

    public ConnectionSshTunnelSettingsForm(final ConnectionSshTunnelSettings configuration) {
        super(configuration);

        initComboBox(authTypeComboBox, SshAuthType.values());
        resetFormChanges();

        authTypeComboBox.addActionListener(e -> {
            showHideFields();
            invalidateTunnelConnectionResult();
        });

        enableDisableFields();
        showHideFields();
        registerComponent(mainPanel);

        addSingleFileChooser(getProject(), keyFileField, txt("cfg.connection.title.SelectPrivateKeyFile"), "");
        testTunnelConnectionPanel.add(testTunnelConnectionControl, BorderLayout.CENTER);
        testTunnelConnectionControl.setAction(this::testTunnelConnection);

        initTunnelConnectionResultInvalidation();
    }

    private void initTunnelConnectionResultInvalidation() {
        onTextChange(hostTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(portTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(userTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(passwordField, e -> invalidateTunnelConnectionResult());
        onTextChange(keyFileField, e -> invalidateTunnelConnectionResult());
        onTextChange(keyPassphraseField, e -> invalidateTunnelConnectionResult());
    }

    private void invalidateTunnelConnectionResult() {
        testTunnelConnectionControl.invalidateOperationResult();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            mackConfigModified();
            Object source = e.getSource();

            if (source == activeCheckBox) {
                enableDisableFields();
                invalidateTunnelConnectionResult();
            }
        };
    }

    private void showHideFields() {
        boolean isKeyPair = getSelection(authTypeComboBox) == SshAuthType.KEY_PAIR;
        passwordField.setVisible(!isKeyPair);
        passwordLabel.setVisible(!isKeyPair);

        privateKeyFileLabel.setVisible(isKeyPair);
        privateKeyPassphraseLabel.setVisible(isKeyPair);
        keyFileField.setVisible(isKeyPair);
        keyPassphraseField.setVisible(isKeyPair);
    }

    private void enableDisableFields() {
        boolean enabled = activeCheckBox.isSelected();
        hostTextField.setEnabled(enabled);
        portTextField.setEnabled(enabled);
        userTextField.setEnabled(enabled);
        authTypeComboBox.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        passwordField.setBackground(enabled ? Colors.getTextFieldBackground() : Colors.getPanelBackground());
        keyFileField.setEnabled(enabled);
        keyPassphraseField.setEnabled(enabled);
        keyPassphraseField.setBackground(enabled ? Colors.getTextFieldBackground() : Colors.getPanelBackground());
        updateTestTunnelConnectionAvailability();
    }

    private void updateTestTunnelConnectionAvailability() {
        testTunnelConnectionControl.setOperationEnabled(activeCheckBox.isSelected());
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionSshTunnelSettings configuration = getConfiguration();
        applyFormChanges(configuration);
    }

    @Override
    public void applyFormChanges(ConnectionSshTunnelSettings configuration) throws ConfigurationException {
        // snapshot old secret before form changes are applied
        Secret[] oldSecrets = configuration.snapshotSecrets();

        boolean enabled = activeCheckBox.isSelected();
        configuration.setActive(enabled);
        configuration.setHost(ConfigurationEditors.validateStringValue(hostTextField, txt("cfg.connection.field.Host"), enabled));
        ConfigurationEditors.validateIntegerValue(portTextField, txt("cfg.connection.field.Port"), enabled, 0, 999999, null);
        configuration.setPort(getText(portTextField));
        configuration.setUser(getText(userTextField));
        SshAuthType authType = getSelection(authTypeComboBox);

        boolean isKeyPair = authType == SshAuthType.KEY_PAIR;
        ConfigurationEditors.validateStringValue(keyFileField.getTextField(), txt("cfg.connection.field.KeyFile"), enabled && isKeyPair);
        //ConfigurationEditorUtil.validateStringInputValue(keyPassphraseField, "Key passphrase", enabled && isKeyPair);

        configuration.setAuthType(authType);
        configuration.setPassword(getPassword(passwordField, configuration.getPassword()));
        configuration.setKeyFile(getText(keyFileField));
        configuration.setKeyPassphrase(getPassword(keyPassphraseField, configuration.getKeyPassphrase()));

        if (!ConfigMonitor.isCloning()) {
            // replace secrets in password store
            configuration.updateSecrets(oldSecrets);
        }
    }

    @Override
    public void resetFormChanges() {
        ConnectionSshTunnelSettings configuration = getConfiguration();
        activeCheckBox.setSelected(configuration.isActive());
        setText(hostTextField, configuration.getHost());
        setText(portTextField, configuration.getPort());
        setText(userTextField, configuration.getUser());
        setPassword(passwordField, configuration.getPassword());
        setSelection(authTypeComboBox, configuration.getAuthType());
        setText(keyFileField, configuration.getKeyFile());
        setPassword(keyPassphraseField, configuration.getKeyPassphrase());
    }

    private void testTunnelConnection() {
        SshTunnelConfig tunnelConfig;
        try {
            ConnectionSettings connectionSettings = getConfiguration().ensureParent();
            ConnectionSettingsForm settingsForm = connectionSettings.getSettingsEditor();
            ConnectionSettings temporarySettings = settingsForm == null ?
                    connectionSettings :
                    settingsForm.getTemporaryConfig();
            tunnelConfig = createTunnelConfig(temporarySettings);
        } catch (ConfigurationException e) {
            testTunnelConnectionControl.showResult(ERROR, getLocalizedMessage(e));
            return;
        }

        String proxyEndpoint = tunnelConfig.getProxyAddress().toString();
        String remoteEndpoint = tunnelConfig.getRemoteAddress().toString();
        testTunnelConnectionControl.execute(
                () -> executeTunnelConnectionTest(tunnelConfig),
                result -> showTunnelConnectionResult(
                        proxyEndpoint,
                        remoteEndpoint,
                        result));
    }

    private Result<Void> executeTunnelConnectionTest(SshTunnelConfig tunnelConfig) {
        try {
            SshConnections.testTunnelConnection(tunnelConfig);
            return new Result<>((Void) null);
        } catch (Exception e) {
            conditionallyLog(e);
            return new Result<>(e);
        } finally {
            clearPassword(tunnelConfig.getProxyPassword());
            clearPassword(tunnelConfig.getKeyPassphrase());
        }
    }

    private void showTunnelConnectionResult(
            String proxyEndpoint,
            String remoteEndpoint,
            Result<Void> result) {
        if (result.isSuccess()) {
            String message = txt(
                    "msg.connection.info.SshTunnelConnectionSuccessful",
                    proxyEndpoint,
                    remoteEndpoint);
            testTunnelConnectionControl.showResult(SUCCESS, message);
        } else {
            String message = txt(
                    "msg.connection.error.SshTunnelConnectionFailed",
                    proxyEndpoint,
                    remoteEndpoint);
            String details = getLocalizedMessage(result.getError());
            testTunnelConnectionControl.showResult(ERROR, message, details);
        }
    }
}
