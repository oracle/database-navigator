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

package com.dbn.ml.ui.source;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.source.MLFileSourceConfig;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.TextFields.onTextChange;

/**
 * Form for CSV file source selection.
 * Follows VectorToolbox pattern (EmbeddingSourceFilesForm).
 */
public class MLSourceFileForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JLabel fileLabel;
    private JLabel delimiterLabel;
    private JLabel hasHeaderLabel;
    private TextFieldWithBrowseButton filePathField;
    private JTextField delimiterField;
    private JCheckBox hasHeaderCheckBox;

    public MLSourceFileForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initFileChooser();
    }

    private void initFileChooser() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withTitle("Select CSV File")
                .withDescription("Select a CSV file for ML training data")
                .withFileFilter(file -> {
                    String extension = file.getExtension();
                    return extension != null && (
                            extension.equalsIgnoreCase("csv") ||
                            extension.equalsIgnoreCase("tsv") ||
                            extension.equalsIgnoreCase("txt")
                    );
                });
        
        filePathField.addBrowseFolderListener(
                "Select CSV File",
                "Select a CSV file containing training data",
                null,
                descriptor
        );
        
        // Notify parent when file changes
        onTextChange(filePathField, e -> notifySourceChanged());
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(fileLabel, filePathField);
        alignerData.registerFieldGroup(delimiterLabel, delimiterField);
        alignerData.registerFieldGroup(hasHeaderLabel, hasHeaderCheckBox);
    }

    @Override
    protected void initValidation() {
        addValidation(filePathField.getTextField(), 
                t -> !t.getText().trim().isEmpty(), 
                "Please select a CSV file");
    }

    private void notifySourceChanged() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm != null) {
            toolboxForm.onSourceChanged();
        }
    }

    public String getSelectedFilePath() {
        return filePathField.getText();
    }

    public String getSelectedDelimiter() {
        String delimiter = delimiterField.getText();
        return (delimiter != null && !delimiter.isEmpty()) ? delimiter : ",";
    }

    @Nullable
    public VirtualFile getSelectedFile() {
        MLFileSourceConfig config = getConfig();
        return config.getFile();
    }

    private MLFileSourceConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLFileSourceConfig();
        return toolboxForm.getMLRequest().getSourceConfig().getFileSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        MLFileSourceConfig config = getConfig();
        filePathField.setText(config.getFilePath() != null ? config.getFilePath() : "");
        delimiterField.setText(config.getDelimiter() != null ? config.getDelimiter() : ",");
        hasHeaderCheckBox.setSelected(config.isHasHeader());
    }

    @Override
    public void applyFormChanges() {
        MLFileSourceConfig config = getConfig();
        config.setFilePath(filePathField.getText());
        config.setDelimiter(delimiterField.getText());
        config.setHasHeader(hasHeaderCheckBox.isSelected());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
