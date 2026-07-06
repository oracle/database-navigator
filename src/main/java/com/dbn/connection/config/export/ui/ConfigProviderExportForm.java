package com.dbn.connection.config.export.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Strings;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ConfigProviderExportManager;
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

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.nio.file.Path;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_INCLUDE_WALLET;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_OUTPUT_FILE;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_WALLET_FILE;
import static com.dbn.connection.config.export.ConfigProviderExportManager.LAST_WRAPPER_KEY;

public class ConfigProviderExportForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;

    private JTextField wrapperKeyTextField;
    private TextFieldWithBrowseButton outputFileTextField;

    private JBCheckBox includeWalletCheckBox;

    private TextFieldWithBrowseButton walletFileTextField;

    private Path outputFile;
    private Path walletFile;

    private final ConfigProviderExportManager exportService;
    private final ConnectionHandler connection;
    private final ConnectionSettings connectionSettings;

    ConfigProviderExportForm(
            @NotNull ConfigProviderExportDialog parent,
            @NotNull ConfigProviderExportManager exportService,
            @Nullable ConnectionHandler connection,
            @NotNull ConnectionSettings connectionSettings) {

        super(parent);
        this.exportService = exportService;
        this.connection = connection;
        this.connectionSettings = connectionSettings;

        // Fail-fast if .form bindings are wrong
        if (mainPanel == null || headerPanel == null || wrapperKeyTextField == null || outputFileTextField == null ||
                includeWalletCheckBox == null || walletFileTextField == null) {
            throw new IllegalStateException("Form binding failed. Check ConfigProviderExportForm.form bindings.");
        }

        initHeaderPanel();
        outputFileTextField.getTextField().setEditable(false);
        walletFileTextField.getTextField().setEditable(false);

        initListeners();
        updateWalletControls();
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
    }

    private void initListeners() {
        outputFileTextField.addActionListener(e -> chooseOutputFile());

        includeWalletCheckBox.addActionListener(e -> {
            updateWalletControls();
            if (!includeWalletCheckBox.isSelected()) {
                walletFile = null;
                walletFileTextField.setText("");
            }
            validateFormFields();
        });

        walletFileTextField.addActionListener(e -> chooseWalletFile());
    }

    @Override
    protected void initValidation() {
        addTextValidation(outputFileTextField.getTextField(), f -> validateOutputFile());
        addTextValidation(walletFileTextField.getTextField(), f -> validateWalletFile());
    }

    private void chooseOutputFile() {
        FileSaverDescriptor descriptor =
                new FileSaverDescriptor("Export JSON", "Choose where to save the JSON file", "json");

        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, getProject());
        com.intellij.openapi.vfs.VirtualFile baseDir = null;
        String basePath = resolveOutputDirectoryPath();
        if (Strings.isEmpty(basePath) && getProject() != null) basePath = getProject().getBasePath();

        if (Strings.isNotEmpty(basePath)) {
            baseDir = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(basePath);
        }

        VirtualFileWrapper wrapper = (baseDir == null)
                ? dialog.save("connection.json")
                : dialog.save(baseDir, "connection.json");

        if (wrapper == null) return;

        outputFile = wrapper.getFile().toPath();
        outputFileTextField.setText(outputFile.toString());
        validateFormFields();
    }

    private void chooseWalletFile() {
        if (!includeWalletCheckBox.isSelected()) return;

        var descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        descriptor.setTitle("Select Wallet File");
        descriptor.setDescription("Select cwallet.sso or ewallet.pem");
        descriptor.withFileFilter(vf -> {
            String n = vf.getName().toLowerCase();
            return n.equals("cwallet.sso") || n.equals("ewallet.pem");
        });

        VirtualFile vf = FileChooser.chooseFile(descriptor, getProject(), null);
        if (vf == null) return;

        walletFile = Path.of(vf.getPath());
        walletFileTextField.setText(walletFile.toString());
        validateFormFields();
    }

    private void updateWalletControls() {
        boolean enabled = includeWalletCheckBox.isSelected();
        walletFileTextField.setEnabled(enabled);
        walletFileTextField.getTextField().setEnabled(enabled);
    }

    private String resolveOutputDirectoryPath() {
        if (outputFile != null) {
            Path parent = outputFile.toAbsolutePath().getParent();
            if (parent != null) return parent.toString();
        }

        String configuredPath = getText(outputFileTextField.getTextField());
        Path configuredFile = toPath(configuredPath);
        if (configuredFile != null) {
            Path parent = configuredFile.toAbsolutePath().getParent();
            if (parent != null) return parent.toString();
        }

        return configuredPath;
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

        return ConfigProviderExportRequest.builder()
                .outputFile(outputFile)
                .formatId("json") // JSON-only UI
                .wrapperKey(key)
                .includeWallet(includeWalletCheckBox.isSelected())
                .walletFile(walletFile)
                .build();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
