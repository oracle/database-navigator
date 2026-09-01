package com.dbn.connection.config.export.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.export.ExportDestination;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.util.CheckBoxes;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.FileChoosers;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ConfigProviderExportManager;
import com.dbn.connection.config.export.ConfigProviderExportRequest;
import com.dbn.connection.config.export.ConfigProviderMapper;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.nio.file.Path;

import static com.dbn.common.export.ExportDestination.CLIPBOARD;
import static com.dbn.common.export.ExportDestination.FILE;
import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.CheckBoxes.installCheckConfirmation;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.setPassword;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Chars.isNotEmpty;
import static com.dbn.common.util.Commons.matchArrays;
import static com.dbn.common.util.FileChoosers.addFileChooser;
import static com.dbn.common.util.FileChoosers.withExtensionFilter;
import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.nls.NlsResources.txt;

public class ConfigProviderExportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;

    private JTextField wrapperKeyTextField;
    private JBCheckBox passwordCheckBox;
    private JLabel databasePasswordLabel;
    private JPasswordField databasePasswordField;
    private TextFieldWithBrowseButton destinationFileTextField;

    private JBCheckBox walletCheckBox;
    private DBNInfoLabel walletInfoLabel;

    private TextFieldWithBrowseButton walletFileTextField;
    private DBNCommentLabel walletFileHintLabel;
    private JLabel walletFileLabel;
    private JComboBox<ExportDestination> destinationComboBox;
    private JLabel destinationFileLabel;
    private DBNInfoLabel configKeyInfoLabel;

    private Path outputFile;
    private Path walletFile;

    private final ConnectionSettings connectionSettings;
    private final boolean walletAvailable;
    private final boolean passwordAvailable;

    ConfigProviderExportForm(
            @NotNull ConfigProviderExportDialog parent,
            @NotNull ConnectionSettings connectionSettings) {

        super(parent);
        this.connectionSettings = connectionSettings;
        this.walletAvailable = ConfigProviderMapper.hasConfiguredWallet(connectionSettings);
        this.passwordAvailable = hasConfiguredDatabasePassword(connectionSettings);

        initHeaderPanel();
        walletInfoLabel.setContent(plain(txt("cfg.connection.text.OracleWalletInfo")));
        destinationFileTextField.getTextField().setEditable(false);
        walletFileTextField.getTextField().setEditable(false);

        ComboBoxes.initComboBox(destinationComboBox, ExportDestination.values());

        configKeyInfoLabel.setContent(plain(txt("cfg.connection.hint.ExportWrapperKey")));
        initListeners();
    }

    private static boolean hasConfiguredDatabasePassword(@NotNull ConnectionSettings connectionSettings) {
        AuthenticationInfo authentication = connectionSettings.getDatabaseSettings().getAuthenticationInfo();
        if (authentication == null) return false;
        if (authentication.getType() != USER_PASSWORD) return false;

        char[] password = authentication.getPassword();
        try {
            return isNotEmpty(password);
        } finally {
            clearPassword(password);
        }
    }

    private void initHeaderPanel() {
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        ConnectionHandler connection = getConnection();
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
        ConfigProviderExportManager exportManager = ConfigProviderExportManager.getInstance();
        StateAttributes state = exportManager.getExportFormState();

        initPersistence(wrapperKeyTextField, state, "last-wrapper-key");
        initPersistence(destinationFileTextField, state, "last-output-file");
        initPersistence(walletCheckBox, state, "last-include-wallet");
        initPersistence(walletFileTextField, state, "last-wallet-file");
        initPersistence(destinationComboBox, state, "last-destination");

        if (Strings.isEmpty(getText(destinationFileTextField))) {
            setText(destinationFileTextField, getDefaultOutputFile().toString());
        }

        outputFile = toPath(getText(destinationFileTextField));
        walletFile = toPath(getText(walletFileTextField));
        updateFieldAvailability();
    }

    private void initListeners() {
        destinationFileTextField.addActionListener(e -> chooseOutputFile());

        onSelectionChange(destinationComboBox, e -> updateFieldAvailability());
        CheckBoxes.onSelectionChange(walletCheckBox, e -> updateFieldAvailability());
        CheckBoxes.onSelectionChange(passwordCheckBox, e -> updateFieldAvailability());

        installCheckConfirmation(passwordCheckBox, getProject(), createPasswordExposureMessage());
        addFileChooser(getProject(), walletFileTextField, tnsFileChooser());
    }

    public static @NotNull FileChooserDescriptor tnsFileChooser() {
        FileChooserDescriptor descriptor = FileChoosers.singleFile().
                withTitle(txt("cfg.connection.title.SelectSsoFile")).
                withDescription(txt("cfg.connection.text.SelectSsoFile"))/*.
                withExtensionFilter("ora")*/;

        return withExtensionFilter(descriptor, "sso");
    }

    private static @NotNull TitledMessage createPasswordExposureMessage() {
        return new TitledMessage(
                MessageType.WARNING,
                txt("msg.connection.title.ExportDatabasePassword"),
                txt("msg.connection.question.ExportDatabasePassword"));
    }

    @Override
    protected void initValidation() {
        addTextValidation(destinationFileTextField.getTextField(), f -> validateDestinationFile());
        addTextValidation(walletFileTextField.getTextField(), f -> validateWalletFile());
        addPasswordValidation(databasePasswordField, password -> !isDatabasePasswordExportSelected() || isNotEmpty(password),
                txt("msg.connection.error.ExportPasswordRequired"));
        addPasswordValidation(databasePasswordField, this::matchesConfiguredDatabasePassword,
                txt("msg.connection.error.ExportPasswordMismatch"));

        addValidation(destinationComboBox, f -> validateDestination());
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
        destinationFileTextField.setText(outputFile.toString());
        validateFormFields();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();

        fieldAdapter.initFieldsVisibility(() -> getExportDestination() == FILE, array(
                destinationFileLabel,
                destinationFileTextField));

        fieldAdapter.initFieldsVisibility(() -> walletAvailable, array(
                walletInfoLabel,
                walletCheckBox));

        fieldAdapter.initFieldsVisibility(() -> walletAvailable && walletCheckBox.isSelected(), array(
                walletFileLabel,
                walletFileTextField,
                walletFileHintLabel));

        fieldAdapter.initFieldsVisibility(() -> passwordAvailable, array(passwordCheckBox));
        fieldAdapter.initFieldsVisibility(() -> passwordAvailable && passwordCheckBox.isSelected(), array(
                databasePasswordLabel,
                databasePasswordField));
    }

    private boolean isDatabasePasswordExportSelected() {
        return passwordAvailable && passwordCheckBox.isSelected();
    }

    private void clearDatabasePasswordFields() {
        char[] password = databasePasswordField.getPassword();
        try {
            setPassword(databasePasswordField, null);
        } finally {
            clearPassword(password);
        }
    }


    private ExportDestination getExportDestination() {
        return getSelection(destinationComboBox);
    }

    private Path getDefaultOutputFile() {
        String baseDir = getProject() == null ? null : getProject().getBasePath();
        if (Strings.isEmpty(baseDir)) baseDir = System.getProperty("user.home");
        return Path.of(baseDir, "connection.json");
    }

    private static Path toPath(String path) {
        return Strings.isEmpty(path) ? null : Path.of(path);
    }

    private String validateDestinationFile() {
        if (getExportDestination() != FILE) return null;
        if (outputFile == null) return "Please choose an output file.";
        return null;
    }

    private String validateDestination() {
        if (getExportDestination() == CLIPBOARD) {
            if (walletCheckBox.isSelected()) {
                return txt("cfg.connection.warning.ClipboardWithWallet");
            }
        }
        return null;
    }

    private String validateWalletFile() {
        if (walletCheckBox.isSelected() && walletFile == null) {
            return "Please choose a wallet file or uncheck 'Include wallet'.";
        }
        return null;
    }

    public ConfigProviderExportRequest getExportRequest() {
        String key = getText(wrapperKeyTextField);
        if (key.isBlank()) key = null;

        char[] databasePassword = isDatabasePasswordExportSelected() ? getPassword(databasePasswordField) : null;

        return ConfigProviderExportRequest.builder()
                .outputFile(getExportDestination() == FILE  ? outputFile : null)
                .destination(getExportDestination())
                .formatId("json") // JSON-only UI
                .wrapperKey(key)
                .includeDatabasePassword(databasePassword != null)
                .databasePassword(databasePassword)
                .includeWallet(walletCheckBox.isSelected())
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
