package com.dbn.connection.config.configprovider.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Strings;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.configprovider.ConfigProviderExportService;
import com.dbn.connection.config.configprovider.ConfigProviderExportRequest;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Path;

import static com.dbn.common.ui.util.TextFields.getText;

public class ConfigProviderExportForm extends DBNFormBase {
    private JPanel mainPanel;

    private JTextField wrapperKeyTextField;
    private TextFieldWithBrowseButton outputFileTextField;

    private JBCheckBox includePasswordCheckBox;
    private JBCheckBox includeWalletCheckBox;

    private TextFieldWithBrowseButton walletFileTextField;

    private JLabel errorLabel;

    private Path outputFile;
    private Path walletFile;

    private final ConnectionSettings connectionSettings;
    private final ConfigProviderExportService exportService;

    ConfigProviderExportForm(
            @NotNull ConfigProviderExportDialog parent,
            @NotNull ConnectionSettings connectionSettings,
            @NotNull ConfigProviderExportService exportService) {

        super(parent);
        this.connectionSettings = connectionSettings;
        this.exportService = exportService;

        // Fail-fast if .form bindings are wrong
        if (mainPanel == null || wrapperKeyTextField == null || outputFileTextField == null ||
                includePasswordCheckBox == null || includeWalletCheckBox == null ||
                walletFileTextField == null) {
            throw new IllegalStateException("Form binding failed. Check ConfigProviderExportForm.form bindings.");
        }

        // Optional error label
        if (errorLabel != null) {
            errorLabel.setIcon(Icons.COMMON_ERROR);
            errorLabel.setVisible(false);
        }

        outputFileTextField.getTextField().setEditable(false);
        walletFileTextField.getTextField().setEditable(false);

        initDefaults();
        initListeners();
        updateWalletControls();
    }

    private void initDefaults() {
        // wrapper key default
        wrapperKeyTextField.setText(exportService.getLastWrapperKey());

        // output file default: lastExportDir -> project base -> user home
        String baseDir = exportService.getLastExportDir();
        if (Strings.isEmpty(baseDir) && getProject() != null) baseDir = getProject().getBasePath();
        if (Strings.isEmpty(baseDir)) baseDir = System.getProperty("user.home");

        outputFile = Path.of(baseDir, "connection.json");
        outputFileTextField.setText(outputFile.toString());

        includePasswordCheckBox.setSelected(exportService.isLastIncludePassword());
        includeWalletCheckBox.setSelected(exportService.isLastIncludeWallet());
    }

    private void initListeners() {
        outputFileTextField.addActionListener(e -> chooseOutputFile());

        includeWalletCheckBox.addActionListener(e -> {
            updateWalletControls();
            if (!includeWalletCheckBox.isSelected()) {
                walletFile = null;
                walletFileTextField.setText("");
            }
        });

        walletFileTextField.addActionListener(e -> chooseWalletFile());    }

    private void chooseOutputFile() {
        FileSaverDescriptor descriptor =
                new FileSaverDescriptor("Export JSON", "Choose where to save the JSON file", "json");

        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, getProject());
        com.intellij.openapi.vfs.VirtualFile baseDir = null;
        String basePath = exportService.getLastExportDir();
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
    }

    private void updateWalletControls() {
        boolean enabled = includeWalletCheckBox.isSelected();
        walletFileTextField.setEnabled(enabled);
        walletFileTextField.getTextField().setEnabled(enabled);
    }

    public void validateEntries(@NotNull Runnable onSuccess) {
        hideError();

        if (outputFile == null) {
            showError("Please choose an output file.");
            return;
        }
        if (includeWalletCheckBox.isSelected() && walletFile == null) {
            showError("Please choose a wallet file or uncheck 'Include wallet'.");
            return;
        }
        onSuccess.run();
    }

    public ConfigProviderExportRequest getExportRequest() {
        String key = Strings.trim(getText(wrapperKeyTextField));
        if (key != null && key.isBlank()) key = null;

        return ConfigProviderExportRequest.builder()
                .outputFile(outputFile)
                .formatId("json") // JSON-only UI
                .wrapperKey(key)
                .includePassword(includePasswordCheckBox.isSelected())
                .includeWallet(includeWalletCheckBox.isSelected())
                .walletFile(walletFile)
                .build();


    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}