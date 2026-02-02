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

package com.dbn.ml.ui.feature;

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.form.field.JComponentFilter.array;

/**
 * Form for selecting features and labels for ML training.
 * Supports both database tables and CSV files as data sources.
 * 
 * For CSV: reads headers from file and displays as column names
 * For DB: loads columns from selected table
 */
public class MLFeatureForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel featuresLabel;
    private JBScrollPane featuresScrollPane;
    private JLabel labelLabel;
    private JLabel label2Label;
    private DBNComboBox<String> labelComboBox;
    private DBNComboBox<String> labelComboBox2;
    
    // Features list (multi-select) - now uses String instead of DBColumn
    private JBList<String> featuresList;
    private DefaultListModel<String> featuresListModel;
    
    // Cached column names (works for both CSV headers and DB columns)
    private List<String> availableColumns = new ArrayList<>();

    public MLFeatureForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    private void initComponents() {
        featuresListModel = new DefaultListModel<>();
        featuresList = new JBList<>(featuresListModel);
        featuresList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        if (featuresScrollPane != null) {
            featuresScrollPane.setViewportView(featuresList);
        }
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        // Features and labels available when columns are loaded
        fieldAdapter.initFieldsAvailability(
                () -> !availableColumns.isEmpty(), 
                array(featuresScrollPane, labelComboBox, labelComboBox2));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(featuresLabel, featuresScrollPane);
        alignerData.registerFieldGroup(labelLabel, labelComboBox);
        alignerData.registerFieldGroup(label2Label, labelComboBox2);
    }

    /**
     * Called when source changes - reload columns from new source.
     * Handles both CSV files and database tables.
     */
    public void refreshColumns() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return;
        
        MLSourceForm sourceForm = toolboxForm.getSourceForm();
        if (sourceForm == null) return;
        
        MLSourceType sourceType = sourceForm.getSelectedSourceType();
        
        if (sourceType == MLSourceType.FILE_SYSTEM) {
            loadColumnsFromCSV(sourceForm);
        } else if (sourceType == MLSourceType.DATABASE_TABLE) {
            loadColumnsFromDatabase();
        }
    }

    /**
     * Loads column names from CSV file headers.
     * Reads file path directly from source form (not config) because config may not be updated yet.
     */
    private void loadColumnsFromCSV(MLSourceForm sourceForm) {
        // Get file path directly from the source form's file field
        String filePath = sourceForm.getSelectedFilePath();
        
        System.out.println("=== loadColumnsFromCSV() ===");
        System.out.println("File path: " + filePath);
        
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("File path is empty, clearing columns");
            clearColumns();
            return;
        }
        
        // Get delimiter directly from source form
        String delimiter = sourceForm.getSelectedDelimiter();
        System.out.println("Delimiter: " + delimiter);
        
        // Read CSV headers in background
        Background.run(() -> {
            try {
                System.out.println("Reading CSV headers from: " + filePath);
                List<String> headers = readCSVHeaders(filePath, delimiter);
                System.out.println("Found " + headers.size() + " headers: " + headers);
                
                // Update UI on EDT
                Dispatch.run(featuresList, () -> {
                    updateColumnsUI(headers);
                });
            } catch (Exception e) {
                System.err.println("Failed to read CSV headers: " + e.getMessage());
                e.printStackTrace();
                Dispatch.run(featuresList, this::clearColumns);
            }
        });
    }

    /**
     * Reads headers from a CSV file.
     */
    private List<String> readCSVHeaders(String filePath, String delimiter) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isEmpty()) {
                return Collections.emptyList();
            }
            
            String[] headers = headerLine.split(delimiter);
            List<String> result = new ArrayList<>(headers.length);
            for (String header : headers) {
                result.add(header.trim());
            }
            return result;
        }
    }

    private String getDelimiter() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return ",";
        
        String delimiter = toolboxForm.getMLRequest().getSourceConfig()
                .getFileSourceConfig().getDelimiter();
        return delimiter != null ? delimiter : ",";
    }

    /**
     * Loads column names from database table.
     */
    private void loadColumnsFromDatabase() {
        DBTable table = getSelectedTable();
        if (table == null) {
            clearColumns();
            return;
        }
        
        // Load columns in background
        Background.run(() -> {
            List<DBColumn> columns = table.getColumns();
            List<String> columnNames = new ArrayList<>(columns.size());
            for (DBColumn column : columns) {
                columnNames.add(column.getName());
            }
            
            // Update UI on EDT
            Dispatch.run(featuresList, () -> {
                updateColumnsUI(columnNames);
            });
        });
    }

    private DBTable getSelectedTable() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return null;
        
        MLSourceForm sourceForm = toolboxForm.getSourceForm();
        if (sourceForm == null) return null;
        
        return sourceForm.getSelectedTable();
    }

    /**
     * Updates the UI with new column names.
     * Called on EDT after loading columns from CSV or DB.
     */
    private void updateColumnsUI(List<String> columns) {
        this.availableColumns = new ArrayList<>(columns);
        
        // Update features list
        featuresListModel.clear();
        for (String column : columns) {
            featuresListModel.addElement(column);
        }
        
        // Update label combo boxes
        ComboBoxes.initComboBox(labelComboBox, columns);
        ComboBoxes.initComboBox(labelComboBox2, columns);
        
        // Add empty option for optional second label
        labelComboBox2.insertItemAt(null, 0);
        labelComboBox2.setSelectedIndex(0);
        
        updateFieldAvailability();
        
        // Restore saved selections if any
        MLFeatureConfig config = getConfig();
        restoreSelections(config);
    }

    private void clearColumns() {
        availableColumns.clear();
        featuresListModel.clear();
        ComboBoxes.initComboBox(labelComboBox, Collections.emptyList());
        ComboBoxes.initComboBox(labelComboBox2, Collections.emptyList());
        updateFieldAvailability();
    }

    private void restoreSelections(MLFeatureConfig config) {
        // Restore feature selections
        List<String> savedFeatures = config.getFeatureColumns();
        if (savedFeatures != null && !savedFeatures.isEmpty()) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < featuresListModel.size(); i++) {
                String column = featuresListModel.get(i);
                if (savedFeatures.contains(column)) {
                    indices.add(i);
                }
            }
            if (!indices.isEmpty()) {
                int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();
                featuresList.setSelectedIndices(indicesArray);
            }
        }
        
        // Restore label selections
        String savedLabel1 = config.getLabelColumn();
        if (savedLabel1 != null && availableColumns.contains(savedLabel1)) {
            ComboBoxes.setSelection(labelComboBox, savedLabel1);
        }
        
        String savedLabel2 = config.getLabelColumn2();
        if (savedLabel2 != null && availableColumns.contains(savedLabel2)) {
            ComboBoxes.setSelection(labelComboBox2, savedLabel2);
        }
    }

    public List<String> getSelectedFeatures() {
        if (featuresList == null) return new ArrayList<>();
        return new ArrayList<>(featuresList.getSelectedValuesList());
    }

    public String getSelectedLabel() {
        return ComboBoxes.getSelection(labelComboBox);
    }

    public String getSelectedLabel2() {
        return ComboBoxes.getSelection(labelComboBox2);
    }

    /**
     * Returns all selected labels (1 or 2)
     */
    public List<String> getSelectedLabels() {
        List<String> labels = new ArrayList<>();
        String label1 = getSelectedLabel();
        String label2 = getSelectedLabel2();
        if (label1 != null && !label1.isEmpty()) labels.add(label1);
        if (label2 != null && !label2.isEmpty()) labels.add(label2);
        return labels;
    }

    private MLFeatureConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLFeatureConfig();
        return toolboxForm.getMLRequest().getFeatureConfig();
    }

    @Override
    public void resetFormChanges() {
        if (featuresListModel == null) return;
        refreshColumns();
    }

    @Override
    public void applyFormChanges() {
        MLFeatureConfig config = getConfig();
        config.setFeatureColumns(getSelectedFeatures());
        config.setLabelColumn(getSelectedLabel());
        config.setLabelColumn2(getSelectedLabel2());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Features & Label";
    }

    @Override
    public String getFormTitleDetail() {
        List<String> features = getSelectedFeatures();
        List<String> labels = getSelectedLabels();
        if (features.isEmpty() && labels.isEmpty()) {
            return null;
        }
        String labelInfo = labels.isEmpty() ? "none" : String.join(", ", labels);
        return features.size() + " features, labels: " + labelInfo;
    }
}
