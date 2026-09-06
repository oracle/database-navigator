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
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Objects;

import static com.dbn.common.ui.util.Focus.onFocusLost;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.ml.model.source.MLSourceType.FILE_SYSTEM;
import static com.dbn.nls.NlsResources.txt;

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
    private boolean sourceTextChanged;

    public MLSourceFileForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initFileChooser();
    }

    private void initFileChooser() {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withTitle(txt("cfg.machineLearning.title.SelectCsvFile"))
                .withDescription(txt("cfg.machineLearning.text.SelectCsvFile"))
                .withFileFilter(file -> {
                    String extension = file.getExtension();
                    return extension != null && (
                            extension.equalsIgnoreCase("csv") ||
                            extension.equalsIgnoreCase("tsv") ||
                            extension.equalsIgnoreCase("txt")
                    );
                });
        
        filePathField.addBrowseFolderListener(
                txt("cfg.machineLearning.title.SelectCsvFile"),
                txt("cfg.machineLearning.text.SelectTrainingCsvFile"),
                null,
                descriptor,
                new TextComponentAccessor<>() {
                    @Override
                    public String getText(JTextField component) {
                        return component.getText();
                    }

                    @Override
                    public void setText(JTextField component, String text) {
                        component.setText(text);
                        notifyTextSourceChanged();
                    }
                }
        );
        
    }

    @Override
    protected void initEventListeners() {
        onTextChange(filePathField, e -> {
            invalidateTextSource();
            if (!filePathField.getTextField().hasFocus()) {
                dispatch(this::notifyTextSourceChanged);
            }
        });
        onTextChange(delimiterField, e -> invalidateTextSource());

        onFocusLost(filePathField.getTextField(), e -> notifyTextSourceChanged());
        onFocusLost(delimiterField, e -> notifyTextSourceChanged());

        hasHeaderCheckBox.addActionListener(e -> notifySourceChanged());
    }

    private void invalidateTextSource() {
        if (sourceTextChanged) return;

        sourceTextChanged = true;
        ensureParentFrom(MLSourceForm.class).notifySourceInvalidated(FILE_SYSTEM);
    }

    private void notifyTextSourceChanged() {
        if (!sourceTextChanged) return;

        sourceTextChanged = false;
        notifySourceChanged();
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
                txt("msg.machineLearning.error.CsvFileRequired"));
    }

    private void notifySourceChanged() {
        ensureParentFrom(MLSourceForm.class).notifySourceChanged(FILE_SYSTEM);
    }

    public String getSelectedFilePath() {
        return filePathField.getText();
    }

    public String getSelectedDelimiter() {
        String delimiter = delimiterField.getText();
        return delimiter == null || delimiter.isEmpty() ? "," : delimiter;
    }

    public boolean isHeaderPresent() {
        return hasHeaderCheckBox.isSelected();
    }

    boolean isConfiguredSourceSelected() {
        MLFileSourceConfig config = getConfig();
        return Objects.equals(getSelectedFilePath(), config.getFilePath()) &&
                Objects.equals(getSelectedDelimiter(), normalizeDelimiter(config.getDelimiter())) &&
                isHeaderPresent() == config.isHasHeader();
    }

    private static String normalizeDelimiter(String delimiter) {
        return delimiter == null || delimiter.isEmpty() ? "," : delimiter;
    }

    @Nullable
    public VirtualFile getSelectedFile() {
        MLFileSourceConfig config = getConfig();
        return config.getFile();
    }

    private MLFileSourceConfig getConfig() {
        return getMLRequest().getSourceConfig().getFileSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        MLFileSourceConfig config = getConfig();
        setTextSilently(filePathField, config.getFilePath() != null ? config.getFilePath() : "");
        setTextSilently(delimiterField, config.getDelimiter() != null ? config.getDelimiter() : ",");
        hasHeaderCheckBox.setSelected(config.isHasHeader());
        sourceTextChanged = false;
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
