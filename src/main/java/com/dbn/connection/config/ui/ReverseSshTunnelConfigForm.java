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

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Chars;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.connection.ssh.SshAuthType;
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

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;

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
    private JPanel mainPanel;


    public ReverseSshTunnelConfigForm(ReverseSshTunnelConfiguration configuration) {
        super(configuration);
        initComboBox(authTypeComboBox, SshAuthType.values());
        authTypeComboBox.addActionListener(e -> showHideFieldsSshAuthTypeComboBox());
        addSingleFileChooser(getProject(), keyFileBrowseInput, txt("cfg.connection.title.SelectPrivateKeyFile"), "");
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

    public void applyFormChanges(ReverseSshTunnelConfiguration configuration) {
        // snapshot old secret before form changes are applied
        Secret[] oldSecrets = configuration.getSecrets();

        configuration.setHost(getText(hostTextField));
        configuration.setPort(getText(portTextField));
        configuration.setUser(getText(userTextField));
        configuration.setAuthType(getSelection(authTypeComboBox));
        configuration.setPassword(passwordField.getPassword());
        configuration.setKeyFile(getText(keyFileBrowseInput));
        configuration.setKeyPassphrase(keyPassPhraseInput.getPassword());
        configuration.setBindHost(getText(bindHostTextField));
        configuration.setBindPort(getText(bindPortTextField));

        if (!ConfigMonitor.isCloning()) {
            // replace secrets in the password store
            configuration.updateSecrets(oldSecrets);
        }
    }

    public void resetFormChanges() {
        ReverseSshTunnelConfiguration configuration = getConfiguration();
        hostTextField.setText(configuration.getHost());
        portTextField.setText(String.valueOf(configuration.getPort()));
        userTextField.setText(configuration.getUser());
        passwordField.setText(Chars.toString(configuration.getPassword()));
        setSelection(authTypeComboBox, configuration.getAuthType());
        keyFileBrowseInput.setText(configuration.getKeyFile());
        keyPassPhraseInput.setText(Chars.toString(configuration.getKeyPassphrase()));
        bindHostTextField.setText(configuration.getBindHost());
        bindPortTextField.setText(String.valueOf(configuration.getBindPort()));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
