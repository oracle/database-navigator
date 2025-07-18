package com.dbn.connection.config.ui;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.connection.ssh.SshAuthType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.common.ui.util.ComboBoxes.*;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;

public class ReverseSshTunnelConfigForm extends DBNFormBase {
    private JLabel sshPasswordLabel;
    private JLabel sshKeyPassPhraseLabel;
    private JTextField sshHostNameTextField;
    private JFormattedTextField sshPortTextField;
    private JTextField sshUserTextField;
    private ComboBox sshAuthenticationTypeComboBox;
    private JPasswordField sshPasswordField;
    private JPasswordField sshKeyPassPhraseInput;
    private JTextField sshBindHost;
    private JFormattedTextField sshBindPort;
    private JLabel sshKeyFileLabel;
    private TextFieldWithBrowseButton sshKeyFileBrowseInput;
    private JPanel mainPanel;


    public ReverseSshTunnelConfigForm(@NotNull DBNForm parentComponent) {
        super(parentComponent);
        initComboBox(sshAuthenticationTypeComboBox, SshAuthType.values());
        sshAuthenticationTypeComboBox.addActionListener(e -> showHideFieldsSshAuthTypeComboBox());
        addSingleFileChooser(getProject(), sshKeyFileBrowseInput, txt("cfg.connection.title.SelectPrivateKeyFile"), "");
    }

    private void showHideFieldsSshAuthTypeComboBox() {
        boolean isKeyPair = getSelection(sshAuthenticationTypeComboBox) == SshAuthType.KEY_PAIR;
        sshPasswordField.setVisible(!isKeyPair);
        sshPasswordLabel.setVisible(!isKeyPair);

        sshKeyFileLabel.setVisible(isKeyPair);
        sshKeyPassPhraseLabel.setVisible(isKeyPair);
        sshKeyFileBrowseInput.setVisible(isKeyPair);
        sshKeyPassPhraseInput.setVisible(isKeyPair);
    }

    public void applyFormChanges(ReverseSshTunnelConfiguration configuration) {
        configuration.setSshHost(sshHostNameTextField.getText());
        configuration.setSshPort(sshPortTextField.getText());
        configuration.setSshUser(sshUserTextField.getText());
        configuration.setSshAuthType((SshAuthType) getSelection(sshAuthenticationTypeComboBox));
        configuration.setSshPassword(sshPasswordField.getPassword());
        configuration.setSshKeyFile(sshKeyFileBrowseInput.getText());
        configuration.setSshKeyPassphrase(sshKeyPassPhraseInput.getPassword());
        configuration.setSshBindHost(sshBindHost.getText());
        configuration.setSshBindPort(sshBindPort.getText());
    }

    public void resetFormChanges(ReverseSshTunnelConfiguration configuration) {
        sshHostNameTextField.setText(configuration.getSshHost());
        sshPortTextField.setText(String.valueOf(configuration.getSshPort()));
        sshUserTextField.setText(configuration.getSshUser());
        sshPasswordField.setText(new String(configuration.getSshPassword()));
        setSelection(sshAuthenticationTypeComboBox, configuration.getSshAuthType());
        sshKeyFileBrowseInput.setText(configuration.getSshKeyFile());
        sshKeyPassPhraseInput.setText(new String(configuration.getSshKeyPassphrase()));
        sshBindHost.setText(configuration.getSshBindHost());
        sshBindPort.setText(String.valueOf(configuration.getSshBindPort()));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
