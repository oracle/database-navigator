package com.dbn.connection.config.export.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.icon.Icons;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ConfigProviderExportManager;
import com.dbn.connection.config.export.ConfigProviderMapper;
import com.dbn.connection.config.export.ConfigProviderExportRequest;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.nio.file.Path;

import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.setPassword;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.util.Chars.isNotEmpty;
import static com.dbn.common.util.Commons.matchArrays;
import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_INCLUDE_WALLET;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_OUTPUT_FILE;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_WALLET_FILE;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_WRAPPER_KEY;
import static com.dbn.nls.NlsResources.txt;

public class ConfigProviderExportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel outputFilePanel;

    private JTextField wrapperKeyTextField;
    private DBNCommentLabel wrapperKeyHintLabel;
    private JPanel databasePasswordPanel;
    private JBCheckBox includeDatabasePasswordCheckBox;
    private DBNCommentLabel databasePasswordWarningLabel;
    private JLabel databasePasswordLabel;
    private JPasswordField databasePasswordField;
    private TextFieldWithBrowseButton outputFileTextField;
    private JRadioButton clipboardDestinationRadioButton;
    private JRadioButton fileDestinationRadioButton;

    private JBCheckBox includeWalletCheckBox;
    private DBNInfoLabel walletInfoLabel;
    private JPanel walletPanel;

    private TextFieldWithBrowseButton walletFileTextField;
    private DBNCommentLabel walletFileHintLabel;
    private JLabel walletFileLabel;

    private Path outputFile;
    private Path walletFile;

    private final ConfigProviderExportManager exportService;
    private final ConnectionHandler connection;
    private final ConnectionSettings connectionSettings;
    private final boolean walletConfigured;
    private final boolean databasePasswordExportAvailable;

    ConfigProviderExportForm(
            @NotNull ConfigProviderExportDialog parent,
            @NotNull ConfigProviderExportManager exportService,
            @Nullable ConnectionHandler connection,
            @NotNull ConnectionSettings connectionSettings) {

        super(parent);
        this.exportService = exportService;
        this.connection = connection;
        this.connectionSettings = connectionSettings;
        this.walletConfigured = ConfigProviderMapper.hasConfiguredWallet(connectionSettings);
        this.databasePasswordExportAvailable = hasConfiguredDatabasePassword(connectionSettings);

        // Fail-fast if .form bindings are wrong
        if (mainPanel == null || headerPanel == null || outputFilePanel == null || wrapperKeyTextField == null || wrapperKeyHintLabel == null ||
                databasePasswordPanel == null || includeDatabasePasswordCheckBox == null || databasePasswordWarningLabel == null ||
                databasePasswordLabel == null || databasePasswordField == null ||
                outputFileTextField == null ||
                clipboardDestinationRadioButton == null || fileDestinationRadioButton == null ||
                includeWalletCheckBox == null || walletInfoLabel == null || walletPanel == null ||
                walletFileTextField == null || walletFileHintLabel == null || walletFileLabel == null) {
            throw new IllegalStateException("Form binding failed. Check ConfigProviderExportForm.form bindings.");
        }

        initHeaderPanel();
        walletInfoLabel.setContent(plain(txt("cfg.connection.text.OracleWalletInfo")));
        outputFileTextField.getTextField().setEditable(false);
        walletFileTextField.getTextField().setEditable(false);

        ButtonGroup destinationGroup = new ButtonGroup();
        destinationGroup.add(clipboardDestinationRadioButton);
        destinationGroup.add(fileDestinationRadioButton);
        fileDestinationRadioButton.setSelected(true);

        initListeners();
        updateDatabasePasswordControls();
        updateWalletControls();
        updateDestinationControls();
    }

    private static boolean hasConfiguredDatabasePassword(@NotNull ConnectionSettings connectionSettings) {
        AuthenticationInfo authentication = connectionSettings.getDatabaseSettings().getAuthenticationInfo();
        if (authentication == null || authentication.getType() != USER_PASSWORD) return false;

        char[] password = authentication.getPassword();
        try {
            return isNotEmpty(password);
        } finally {
            clearPassword(password);
        }
    }

    private void initHeaderPanel() {
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        DBNHeaderForm headerForm = connection == null ?
                new DBNHeaderForm(
                        this,
                        databaseSettings.getName(),
                        Icons.CONNECTION_NEW,
                        connectionSettings.getDetailSettings().getEnvironmentType().getColor()) :
                new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    @Override
    protected void initStatePersistence() {
        StateAttributes state = exportService.getExportFormState();

        initPersistence(wrapperKeyTextField, state, LAST_WRAPPER_KEY);
        initPersistence(outputFileTextField.getTextField(), state, LAST_OUTPUT_FILE);
        initPersistence(includeWalletCheckBox, state, LAST_INCLUDE_WALLET);
        initPersistence(walletFileTextField.getTextField(), state, LAST_WALLET_FILE);

        if (Strings.isEmpty(getText(outputFileTextField.getTextField()))) {
            outputFileTextField.setText(getDefaultOutputFile().toString());
        }

        outputFile = toPath(getText(outputFileTextField.getTextField()));
        walletFile = toPath(getText(walletFileTextField.getTextField()));
        updateWalletControls();
        updateDestinationControls();
    }

    private void initListeners() {
        outputFileTextField.addActionListener(e -> chooseOutputFile());

        clipboardDestinationRadioButton.addActionListener(e -> updateDestinationControls());
        fileDestinationRadioButton.addActionListener(e -> updateDestinationControls());

        includeWalletCheckBox.addActionListener(e -> {
            updateWalletControls();
            updateDestinationControls();
            if (!includeWalletCheckBox.isSelected()) {
                walletFile = null;
                walletFileTextField.setText("");
            }
            validateFormFields();
        });

        includeDatabasePasswordCheckBox.addActionListener(e -> {
            if (includeDatabasePasswordCheckBox.isSelected() && !confirmDatabasePasswordExport()) {
                includeDatabasePasswordCheckBox.setSelected(false);
            }
            if (!includeDatabasePasswordCheckBox.isSelected()) {
                clearDatabasePasswordFields();
            }
            updateDatabasePasswordControls();
            validateFormFields();
        });

        walletFileTextField.addActionListener(e -> chooseWalletFile());
    }

    @Override
    protected void initValidation() {
        addTextValidation(outputFileTextField.getTextField(), f -> validateOutputFile());
        addTextValidation(walletFileTextField.getTextField(), f -> validateWalletFile());
        addPasswordValidation(databasePasswordField, password -> !isDatabasePasswordExportSelected() || isNotEmpty(password),
                txt("msg.connection.error.ExportPasswordRequired"));
        addPasswordValidation(databasePasswordField, this::matchesConfiguredDatabasePassword,
                txt("msg.connection.error.ExportPasswordMismatch"));
    }

    private boolean matchesConfiguredDatabasePassword(char[] password) {
        if (!isDatabasePasswordExportSelected() || !isNotEmpty(password)) return true;

        AuthenticationInfo authentication = connectionSettings.getDatabaseSettings().getAuthenticationInfo();
        char[] configuredPassword = authentication == null ? null : authentication.getPassword();
        try {
            return matchArrays(configuredPassword, password);
        } finally {
            clearPassword(configuredPassword);
        }
    }

    private void chooseOutputFile() {
        FileSaverDescriptor descriptor =
                new FileSaverDescriptor("Export JSON", "Choose where to save the JSON file", "json");

        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, getProject());
        VirtualFileWrapper wrapper = dialog.save((VirtualFile) null, "connection.json");

        if (wrapper == null) return;

        VirtualFile file = wrapper.getVirtualFile(true);
        if (file == null) return;

        outputFile = Path.of(file.getPath());
        outputFileTextField.setText(outputFile.toString());
        validateFormFields();
    }

    private void chooseWalletFile() {
        if (!includeWalletCheckBox.isSelected()) return;

        var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        descriptor.setTitle("Select Wallet File");
        descriptor.setDescription("Select cwallet.sso");
        descriptor.withFileFilter(vf -> {
            String n = vf.getName().toLowerCase();
            return n.equals("cwallet.sso");
        });

        VirtualFile vf = FileChooser.chooseFile(descriptor, getProject(), null);
        if (vf == null) return;

        walletFile = Path.of(vf.getPath());
        walletFileTextField.setText(walletFile.toString());
        validateFormFields();
    }

    private void updateWalletControls() {
        if (!walletConfigured) {
            includeWalletCheckBox.setSelected(false);
            walletFile = null;
            walletFileTextField.setText("");
        }

        boolean enabled = walletConfigured && includeWalletCheckBox.isSelected();
        walletPanel.setVisible(walletConfigured);
        walletFileLabel.setVisible(enabled);
        walletFileTextField.setVisible(enabled);
        walletFileTextField.setEnabled(enabled);
        walletFileTextField.getTextField().setEnabled(enabled);
        walletFileHintLabel.setVisible(enabled);
    }

    private void updateDatabasePasswordControls() {
        boolean includePassword = isDatabasePasswordExportSelected();
        databasePasswordPanel.setVisible(databasePasswordExportAvailable);
        includeDatabasePasswordCheckBox.setVisible(databasePasswordExportAvailable);
        databasePasswordWarningLabel.setVisible(includePassword);
        databasePasswordLabel.setVisible(includePassword);
        databasePasswordField.setVisible(includePassword);
    }

    private boolean isDatabasePasswordExportSelected() {
        return databasePasswordExportAvailable && includeDatabasePasswordCheckBox.isSelected();
    }

    private boolean confirmDatabasePasswordExport() {
        int option = Messages.showAcknowledgementDialog(
                getProject(),
                txt("msg.connection.title.ExportDatabasePassword"),
                txt("msg.connection.question.ExportDatabasePassword"),
                Messages.OPTIONS_YES_NO,
                1,
                null);
        return option == 0;
    }

    private void clearDatabasePasswordFields() {
        char[] password = databasePasswordField.getPassword();
        try {
            setPassword(databasePasswordField, null);
        } finally {
            clearPassword(password);
        }
    }

    private void updateDestinationControls() {
        boolean clipboardAvailable = !includeWalletCheckBox.isSelected();
        clipboardDestinationRadioButton.setEnabled(clipboardAvailable);
        clipboardDestinationRadioButton.setToolTipText(clipboardAvailable ? null :
                txt("cfg.connection.tooltip.ClipboardWithWallet"));

        if (!clipboardAvailable && clipboardDestinationRadioButton.isSelected()) {
            fileDestinationRadioButton.setSelected(true);
        }

        boolean fileDestination = fileDestinationRadioButton.isSelected();
        outputFilePanel.setVisible(fileDestination);
        mainPanel.revalidate();
        mainPanel.repaint();
        validateFormFields();
    }

    private Path getDefaultOutputFile() {
        String baseDir = getProject() == null ? null : getProject().getBasePath();
        if (Strings.isEmpty(baseDir)) baseDir = System.getProperty("user.home");
        return Path.of(baseDir, "connection.json");
    }

    private static Path toPath(String path) {
        return Strings.isEmpty(path) ? null : Path.of(path);
    }

    private String validateOutputFile() {
        if (clipboardDestinationRadioButton.isSelected()) return null;
        if (outputFile == null) {
            return "Please choose an output file.";
        }
        return null;
    }

    private String validateWalletFile() {
        if (includeWalletCheckBox.isSelected() && walletFile == null) {
            return "Please choose a wallet file or uncheck 'Include wallet'.";
        }
        return null;
    }

    public ConfigProviderExportRequest getExportRequest() {
        String key = Strings.trim(getText(wrapperKeyTextField));
        if (key != null && key.isBlank()) key = null;

        char[] databasePassword = isDatabasePasswordExportSelected() ? getPassword(databasePasswordField) : null;

        return ConfigProviderExportRequest.builder()
                .outputFile(fileDestinationRadioButton.isSelected() ? outputFile : null)
                .destination(clipboardDestinationRadioButton.isSelected() ?
                        ConfigProviderExportRequest.Destination.CLIPBOARD :
                        ConfigProviderExportRequest.Destination.FILE)
                .formatId("json") // JSON-only UI
                .wrapperKey(key)
                .includeDatabasePassword(databasePassword != null)
                .databasePassword(databasePassword)
                .includeWallet(includeWalletCheckBox.isSelected())
                .walletFile(walletFile)
                .build();
    }

    @Override
    public void disposeInner() {
        clearDatabasePasswordFields();
        super.disposeInner();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
