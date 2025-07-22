package com.dbn.connection.config.ui;

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Commons;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.connection.ssh.SshAuthType;
import com.dbn.credentials.Secret;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

import static com.dbn.common.ui.util.ComboBoxes.*;
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

        configuration.setHost(hostTextField.getText());
        configuration.setPort(portTextField.getText());
        configuration.setUser(userTextField.getText());
        configuration.setAuthType(getSelection(authTypeComboBox));
        configuration.setPassword(Commons.nvln(passwordField.getPassword(), (char[]) null));
        configuration.setKeyFile(keyFileBrowseInput.getText());
        configuration.setKeyPassphrase(Commons.nvln(keyPassPhraseInput.getPassword(), (char[]) null));
        configuration.setBindHost(bindHostTextField.getText());
        configuration.setBindPort(bindPortTextField.getText());

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
        char[] password = configuration.getPassword();
        passwordField.setText(password == null ? "" : new String(password));
        setSelection(authTypeComboBox, configuration.getAuthType());
        keyFileBrowseInput.setText(configuration.getKeyFile());
        char[] keyPassPhrase = configuration.getKeyPassphrase();
        keyPassPhraseInput.setText(keyPassPhrase == null ? "" : new String(keyPassPhrase));
        bindHostTextField.setText(configuration.getBindHost());
        bindPortTextField.setText(String.valueOf(configuration.getBindPort()));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
