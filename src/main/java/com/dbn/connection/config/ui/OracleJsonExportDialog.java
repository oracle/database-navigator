package com.dbn.connection.config.ui;

import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class OracleJsonExportDialog extends DialogWrapper {
    private final Project project;

    private final JBTextField keyField = new JBTextField();

    private final JBTextField outputFileField = new JBTextField();
    private Path outputFile;

    private final JBCheckBox includePassword =
            new JBCheckBox("Include password — writes sensitive data to disk");

    private final JBCheckBox includeWallet =
            new JBCheckBox("Include wallet — writes sensitive data to disk");

    private final JBTextField walletFileField = new JBTextField();
    private Path walletFile;

    public OracleJsonExportDialog(Project project, boolean passwordEligible) {
        super(project, true);
        this.project = project;

        setTitle("Export JSON");

        String basePath = project.getBasePath();
        java.nio.file.Path defaultPath;

        if (basePath != null) {
            defaultPath = java.nio.file.Path.of(basePath, "connection.json");
        } else {
            defaultPath = java.nio.file.Path.of(System.getProperty("user.home"), "connection.json");
        }

        this.outputFile = defaultPath;
        this.outputFileField.setText(defaultPath.toString());

        includePassword.setEnabled(passwordEligible);
        walletFileField.setEditable(false);
        outputFileField.setEditable(false);

        includeWallet.addActionListener(e -> updateWalletControls());
        updateWalletControls();

        init();
    }

    private void updateWalletControls() {
        boolean enabled = includeWallet.isSelected();
        walletFileField.setEnabled(enabled);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JButton browseOutput = new JButton("Browse...");
        browseOutput.addActionListener(e -> chooseOutputFile());

        JButton browseWallet = new JButton("Browse...");
        browseWallet.addActionListener(e -> chooseWalletFile());

        JPanel outputRow = new JPanel(new BorderLayout(8, 0));
        outputRow.add(outputFileField, BorderLayout.CENTER);
        outputRow.add(browseOutput, BorderLayout.EAST);

        JPanel walletRow = new JPanel(new BorderLayout(8, 0));
        walletRow.add(walletFileField, BorderLayout.CENTER);
        walletRow.add(browseWallet, BorderLayout.EAST);

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Optional key:", keyField)
                .addLabeledComponent("Save JSON to:", outputRow)
                .addComponent(includePassword)
                .addComponent(includeWallet)
                .addLabeledComponent("Wallet file:", walletRow)
                .getPanel();
    }

    private void chooseOutputFile() {
        FileSaverDescriptor descriptor =
                new FileSaverDescriptor("Export JSON", "Choose where to save the JSON file", "json");

        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);

        VirtualFile baseDir = null;
        String basePath = project.getBasePath();
        if (basePath != null) baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);

        VirtualFileWrapper wrapper = dialog.save(baseDir, "connection.json");
        if (wrapper == null) return;

        outputFile = wrapper.getFile().toPath();
        outputFileField.setText(outputFile.toString());
    }

    private void chooseWalletFile() {
        if (!includeWallet.isSelected()) return;

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        descriptor.setTitle("Select Wallet File");
        descriptor.setDescription("Select cwallet.sso or ewallet.pem");
        descriptor.withFileFilter(file -> {
            String n = file.getName().toLowerCase();
            return n.equals("cwallet.sso") || n.equals("ewallet.pem");
        });

        VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
        if (file == null) return;

        walletFile = Path.of(file.getPath());
        walletFileField.setText(walletFile.toString());
    }

    @Override
    protected void doOKAction() {
        // Require output file
        if (outputFile == null) {
            JOptionPane.showMessageDialog(getContentPanel(),
                    "Please choose where to save the JSON file.",
                    "Export JSON",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Require wallet file if wallet is selected
        if (includeWallet.isSelected() && walletFile == null) {
            JOptionPane.showMessageDialog(getContentPanel(),
                    "Please select a wallet file (cwallet.sso or ewallet.pem) or uncheck 'Include wallet'.",
                    "Export JSON",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        super.doOKAction();
    }

    public @Nullable String getKeyOrNull() {
        String k = keyField.getText();
        if (k == null) return null;
        k = k.trim();
        return k.isEmpty() ? null : k;
    }

    public Path getOutputFile() {
        return outputFile;
    }

    public boolean isIncludePassword() {
        return includePassword.isSelected();
    }

    public boolean isIncludeWallet() {
        return includeWallet.isSelected();
    }

    public @Nullable Path getWalletFile() {
        return walletFile;
    }
}