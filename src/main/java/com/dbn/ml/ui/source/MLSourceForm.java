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
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.intellij.openapi.Disposable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

/**
 * Parent form for ML data source selection.
 * 
 * Swaps between child forms based on selected source type:
 * - DATABASE_TABLE -> MLSourceTableForm
 * - FILE_SYSTEM -> MLSourceFileForm
 */
public class MLSourceForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JPanel dataPanel;
    private JLabel sourceTypeLabel;
    private DBNComboBox<MLSourceType> sourceTypeComboBox;
    
    // Child forms
    private MLSourceTableForm tableForm;
    private MLSourceFileForm fileForm;

    public MLSourceForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComboBox();
        initDataPanel();
    }

    private void initComboBox() {
        ComboBoxes.initComboBox(sourceTypeComboBox, MLSourceType.values());
        ComboBoxes.setSelection(sourceTypeComboBox, MLSourceType.DATABASE_TABLE);
    }

    private void initDataPanel() {
        ConnectionHandler connection = getConnection();
        tableForm = new MLSourceTableForm(this, connection);
        fileForm = new MLSourceFileForm(this, connection);
        updateSourceForm();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(sourceTypeLabel, sourceTypeComboBox);
        alignerData.registerForms(tableForm);
        alignerData.registerForms(fileForm);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(sourceTypeComboBox, t -> updateSourceForm());
    }

    private void updateSourceForm() {
        MLSourceType sourceType = getSelectedSourceType();
        dataPanel.removeAll();
        
        if (sourceType == MLSourceType.FILE_SYSTEM) {
            dataPanel.add(fileForm.getComponent());
        } else if (sourceType == MLSourceType.DATABASE_TABLE) {
            dataPanel.add(tableForm.getComponent());
        }
        
        dataPanel.revalidate();
        dataPanel.repaint();
        validateFormFields();
    }

    public MLSourceType getSelectedSourceType() {
        return ComboBoxes.getSelection(sourceTypeComboBox);
    }

    /**
     * Get selected schema (only valid for DATABASE_TABLE source type)
     */
    public DBSchema getSelectedSchema() {
        return tableForm != null ? tableForm.getSelectedSchema() : null;
    }

    /**
     * Get selected table (only valid for DATABASE_TABLE source type)
     */
    public DBTable getSelectedTable() {
        return tableForm != null ? tableForm.getSelectedTable() : null;
    }

    /**
     * Get selected file path (only valid for FILE_SYSTEM source type)
     */
    public String getSelectedFilePath() {
        return fileForm != null ? fileForm.getSelectedFilePath() : null;
    }

    /**
     * Get selected delimiter (only valid for FILE_SYSTEM source type)
     */
    public String getSelectedDelimiter() {
        return fileForm != null ? fileForm.getSelectedDelimiter() : ",";
    }

    private MLSourceConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLSourceConfig();
        return toolboxForm.getMLRequest().getSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        MLSourceConfig config = getConfig();
        ComboBoxes.setSelection(sourceTypeComboBox, config.getSourceType());
        tableForm.resetFormChanges();
        fileForm.resetFormChanges();
        updateSourceForm();
    }

    @Override
    public void applyFormChanges() {
        MLSourceConfig config = getConfig();
        config.setSourceType(getSelectedSourceType());
        tableForm.applyFormChanges();
        fileForm.applyFormChanges();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Data Source";
    }

    @Override
    public String getFormTitleDetail() {
        MLSourceType sourceType = getSelectedSourceType();
        String sourceTypeName = sourceType == null ? "" : sourceType.getName();

        if (sourceType == MLSourceType.FILE_SYSTEM) {
            String filePath = fileForm.getSelectedFilePath();
            if (filePath != null && !filePath.isEmpty()) {
                // Show just filename, not full path
                int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
                String fileName = lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;
                return sourceTypeName + " - " + fileName;
            }
        }

        if (sourceType == MLSourceType.DATABASE_TABLE) {
            DBSchema schema = getSelectedSchema();
            DBTable table = getSelectedTable();
            if (schema != null && table != null) {
                return sourceTypeName + " - " + schema.getName() + "." + table.getName();
            }
        }
        
        return sourceTypeName;
    }
}
