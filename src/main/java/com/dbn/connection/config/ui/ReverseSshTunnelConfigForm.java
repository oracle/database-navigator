package com.dbn.connection.config.ui;

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.ui.ConfigurationEditorForm;
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
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;

public class ReverseSshTunnelConfigForm extends ConfigurationEditorForm<ReverseSshTunnelConfiguration> {
    private JLabel sshPasswordLabel;
    private JLabel sshKeyPassPhraseLabel;
    private JTextField sshHostNameTextField;
    private JFormattedTextField sshPortTextField;
    private JTextField sshUserTextField;
    private ComboBox<SshAuthType> sshAuthenticationTypeComboBox;
    private JPasswordField sshPasswordField;
    private JPasswordField sshKeyPassPhraseInput;
    private JTextField sshBindHost;
    private JFormattedTextField sshBindPort;
    private JLabel sshKeyFileLabel;
    private TextFieldWithBrowseButton sshKeyFileBrowseInput;
    private JPanel mainPanel;


    public ReverseSshTunnelConfigForm(ReverseSshTunnelConfiguration configuration) {
        super(configuration);
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

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ReverseSshTunnelConfiguration configuration = getConfiguration();
        applyFormChanges(configuration);
    }

    public void applyFormChanges(ReverseSshTunnelConfiguration configuration) {
        // snapshot old secret before form changes are applied
        Secret[] oldSecrets = configuration.getSecrets();

        configuration.setSshHost(sshHostNameTextField.getText());
        configuration.setSshPort(sshPortTextField.getText());
        configuration.setSshUser(sshUserTextField.getText());
        configuration.setSshAuthType(getSelection(sshAuthenticationTypeComboBox));
        configuration.setSshPassword(sshPasswordField.getPassword());
        configuration.setSshKeyFile(sshKeyFileBrowseInput.getText());
        configuration.setSshKeyPassphrase(sshKeyPassPhraseInput.getPassword());
        configuration.setSshBindHost(sshBindHost.getText());
        configuration.setSshBindPort(sshBindPort.getText());

        if (!ConfigMonitor.isCloning()) {
            // replace secrets in the password store
            configuration.updateSecrets(oldSecrets);
        }
    }

    public void resetFormChanges() {
        ReverseSshTunnelConfiguration configuration = getConfiguration();
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
