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
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.nls.NlsResources.txt;

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
    private MLSourceCloudForm cloudForm;

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
        cloudForm = new MLSourceCloudForm(this, connection);
        updateSourceForm();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(sourceTypeLabel, sourceTypeComboBox);
        alignerData.registerForms(tableForm);
        alignerData.registerForms(fileForm);
        alignerData.registerForms(cloudForm);
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
        } else if (sourceType == MLSourceType.OBJECT_STORAGE) {
            dataPanel.add(cloudForm.getComponent());
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
     * Get selected delimiter (only valid for FILE_SYSTEM or OBJECT_STORAGE source type)
     */
    public String getSelectedDelimiter() {
        MLSourceType sourceType = getSelectedSourceType();
        if (sourceType == MLSourceType.OBJECT_STORAGE) {
            return cloudForm != null ? cloudForm.getSelectedDelimiter() : ",";
        }
        return fileForm != null ? fileForm.getSelectedDelimiter() : ",";
    }

    /**
     * Get selected cloud URI (only valid for OBJECT_STORAGE source type)
     */
    public String getSelectedCloudUri() {
        return cloudForm != null ? cloudForm.getSelectedUri() : null;
    }

    /**
     * Get selected credential name (only valid for OBJECT_STORAGE source type)
     */
    public String getSelectedCredential() {
        return cloudForm != null ? cloudForm.getSelectedCredential() : null;
    }

    /**
     * Get discovered column names from cloud source (only valid for OBJECT_STORAGE source type)
     */
    public List<String> getCloudDiscoveredColumns() {
        return cloudForm != null ? cloudForm.getDiscoveredColumns() : Collections.emptyList();
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
        cloudForm.resetFormChanges();
        updateSourceForm();
    }

    @Override
    public void applyFormChanges() {
        MLSourceConfig config = getConfig();
        config.setSourceType(getSelectedSourceType());
        tableForm.applyFormChanges();
        fileForm.applyFormChanges();
        cloudForm.applyFormChanges();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return txt("cfg.machineLearning.title.DataSource");
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
                return txt("app.shared.text.ContextQualifiedName", sourceTypeName, fileName);
            }
        }

        if (sourceType == MLSourceType.DATABASE_TABLE) {
            DBSchema schema = getSelectedSchema();
            DBTable table = getSelectedTable();
            if (schema != null && table != null) {
                return txt("app.shared.text.ContextQualifiedName", sourceTypeName, schema.getName() + "." + table.getName());
            }
        }

        if (sourceType == MLSourceType.OBJECT_STORAGE) {
            String uri = getSelectedCloudUri();
            if (uri != null && !uri.isEmpty()) {
                int lastSlash = uri.lastIndexOf('/');
                String objectName = lastSlash >= 0 ? uri.substring(lastSlash + 1) : uri;
                if (objectName.length() > 40) {
                    objectName = "..." + objectName.substring(objectName.length() - 37);
                }
                return txt("app.shared.text.ContextQualifiedName", sourceTypeName, objectName);
            }
        }

        return sourceTypeName;
    }
}
