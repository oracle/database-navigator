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

package com.dbn.connection.config.ui;

import com.dbn.common.Result;
import com.dbn.common.network.NetworkAddress;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.options.ui.ConfigurationEditors;
import com.dbn.common.ui.panel.DBNAsyncOperationPanel;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.connection.ssh.SshAuthType;
import com.dbn.connection.ssh.SshTunnelConfig;
import com.dbn.credentials.Secret;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;

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
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.connection.ssh.SshAuthType.KEY_PAIR;
import static com.dbn.connection.ssh.SshConnections.testReverseTunnelConnection;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ReverseSshTunnelConfigForm extends ConfigurationEditorForm<ReverseSshTunnelConfiguration> {
    private JLabel passwordLabel;
    private JLabel sshKeyPassPhraseLabel;
    private JTextField hostTextField;
    private JFormattedTextField portTextField;
    private JTextField userTextField;
    private ComboBox<SshAuthType> authTypeComboBox;
    private JPasswordField passwordField;
    private JPasswordField keyPassPhraseInput;
    private JTextField bindHostTextField;
    private JFormattedTextField bindPortTextField;
    private JLabel sshKeyFileLabel;
    private TextFieldWithBrowseButton keyFileBrowseInput;
    private JPanel testTunnelConnectionPanel;
    private JPanel mainPanel;

    private final DBNAsyncOperationPanel testTunnelConnectionControl = new DBNAsyncOperationPanel(
            txt("cfg.connection.link.TestTunnelConnection"),
            txt("prc.connection.title.TestingTunnelConnection"));

    public ReverseSshTunnelConfigForm(ReverseSshTunnelConfiguration configuration) {
        super(configuration);
        initComboBox(authTypeComboBox, SshAuthType.values());
        authTypeComboBox.addActionListener(e -> {
            showHideFieldsSshAuthTypeComboBox();
            invalidateTunnelConnectionResult();
        });
        addSingleFileChooser(getProject(), keyFileBrowseInput, txt("cfg.connection.title.SelectPrivateKeyFile"), "");
        testTunnelConnectionPanel.add(testTunnelConnectionControl, BorderLayout.CENTER);
        testTunnelConnectionControl.setAction(this::testTunnelConnection);

        initTunnelConnectionResultInvalidation();
    }

    private void initTunnelConnectionResultInvalidation() {
        onTextChange(hostTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(portTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(userTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(passwordField, e -> invalidateTunnelConnectionResult());
        onTextChange(keyFileBrowseInput, e -> invalidateTunnelConnectionResult());
        onTextChange(keyPassPhraseInput, e -> invalidateTunnelConnectionResult());
        onTextChange(bindHostTextField, e -> invalidateTunnelConnectionResult());
        onTextChange(bindPortTextField, e -> invalidateTunnelConnectionResult());
    }

    private void invalidateTunnelConnectionResult() {
        testTunnelConnectionControl.invalidateOperationResult();
    }

    private void showHideFieldsSshAuthTypeComboBox() {
        boolean isKeyPair = getSelection(authTypeComboBox) == SshAuthType.KEY_PAIR;
        passwordField.setVisible(!isKeyPair);
        passwordLabel.setVisible(!isKeyPair);

        sshKeyFileLabel.setVisible(isKeyPair);
        sshKeyPassPhraseLabel.setVisible(isKeyPair);
        keyFileBrowseInput.setVisible(isKeyPair);
        keyPassPhraseInput.setVisible(isKeyPair);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ReverseSshTunnelConfiguration configuration = getConfiguration();
        applyFormChanges(configuration);
    }

    public void applyFormChanges(ReverseSshTunnelConfiguration configuration) throws ConfigurationException {
        // snapshot old secret before form changes are applied
        Secret[] oldSecrets = configuration.snapshotSecrets();

        String bindHost = getText(bindHostTextField);
        ReverseSshTunnelConfiguration.validateBindHost(bindHost);

        configuration.setHost(getText(hostTextField));
        configuration.setPort(getText(portTextField));
        configuration.setUser(getText(userTextField));
        configuration.setAuthType(getSelection(authTypeComboBox));
        configuration.setPassword(getPassword(passwordField, configuration.getPassword()));
        configuration.setKeyFile(getText(keyFileBrowseInput));
        configuration.setKeyPassphrase(getPassword(keyPassPhraseInput, configuration.getKeyPassphrase()));
        configuration.setBindHost(bindHost);
        configuration.setBindPort(getText(bindPortTextField));

        if (!ConfigMonitor.isCloning()) {
            // replace secrets in the password store
            configuration.updateSecrets(oldSecrets);
        }
    }

    public void resetFormChanges() {
        ReverseSshTunnelConfiguration configuration = getConfiguration();
        setText(hostTextField, configuration.getHost());
        setText(portTextField, String.valueOf(configuration.getPort()));
        setText(userTextField, configuration.getUser());
        setPassword(passwordField, configuration.getPassword());
        setSelection(authTypeComboBox, configuration.getAuthType());
        setText(keyFileBrowseInput, configuration.getKeyFile());
        setPassword(keyPassPhraseInput, configuration.getKeyPassphrase());
        setText(bindHostTextField, configuration.getBindHost());
        setText(bindPortTextField, String.valueOf(configuration.getBindPort()));
    }

    boolean isHostEmpty() {
        return isEmptyOrSpaces(getText(hostTextField));
    }

    void initializeHost(String host) {
        if (isHostEmpty() && isNotEmptyOrSpaces(host)) {
            setText(hostTextField, host);
        }
    }

    private void testTunnelConnection() {
        SshTunnelConfig tunnelConfig;
        try {
            tunnelConfig = createTunnelConfig();
        } catch (ConfigurationException e) {
            testTunnelConnectionControl.showResult(ERROR, getLocalizedMessage(e));
            return;
        }

        String endpoint = tunnelConfig.getProxyAddress().toString();
        testTunnelConnectionControl.execute(
                () -> testTunnelConnection(tunnelConfig),
                result -> showTunnelConnectionResult(endpoint, result));
    }

    private SshTunnelConfig createTunnelConfig() throws ConfigurationException {
        String host = ConfigurationEditors.validateStringValue(
                hostTextField, txt("cfg.connection.field.Host"), true);
        int port = ConfigurationEditors.validateIntegerValue(
                portTextField, txt("cfg.connection.field.Port"), true, 1, 65535, null);
        String user = getText(userTextField);

        SshAuthType authType = getSelection(authTypeComboBox);
        boolean keyPair = authType == KEY_PAIR;
        String keyFile = ConfigurationEditors.validateStringValue(
                keyFileBrowseInput.getTextField(), txt("cfg.connection.field.KeyFile"), keyPair);

        String bindHost = getText(bindHostTextField);
        ReverseSshTunnelConfiguration.validateBindHost(bindHost);
        int bindPort = ConfigurationEditors.validateIntegerValue(
                bindPortTextField, txt("cfg.connection.field.Port"), true, 0, 65535, null);

        ReverseSshTunnelConfiguration configuration = getConfiguration();
        return new SshTunnelConfig(
                new NetworkAddress(host, port),
                new NetworkAddress(bindHost, bindPort),
                authType,
                user,
                getPassword(passwordField, configuration.getPassword()),
                keyFile,
                getPassword(keyPassPhraseInput, configuration.getKeyPassphrase()));
    }

    private Result<Void> testTunnelConnection(SshTunnelConfig tunnelConfig) {
        try {
            testReverseTunnelConnection(tunnelConfig);
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
            String endpoint,
            Result<Void> result) {
        if (result.isSuccess()) {
            testTunnelConnectionControl.showResult(
                    SUCCESS,
                    txt("msg.connection.info.ReverseSshTunnelConnectionSuccessful", endpoint));
        } else {
            String message = txt("msg.connection.error.ReverseSshTunnelConnectionFailed", endpoint);
            String details = getLocalizedMessage(result.getError());
            testTunnelConnectionControl.showResult(ERROR, message, details);
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
