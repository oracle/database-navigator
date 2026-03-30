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
import lombok.extern.slf4j.Slf4j;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import java.awt.FlowLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.ui.form.field.JComponentFilter.array;

/**
 * Form for selecting features and labels for ML training.
 * Supports both database tables and CSV files as data sources.
 *
 * For CSV: reads headers from file and displays as column names
 * For DB: loads columns from selected table
 */
@Slf4j
public class MLFeatureForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel featuresLabel;
    private JBScrollPane featuresScrollPane;
    private JLabel labelLabel;
    private DBNComboBox<String> labelComboBox;
    private JPanel partitionLabelPanel;
    private JCheckBox partitionEnabledCheckBox;
    private JBScrollPane partitionScrollPane;

    // Features list (multi-select)
    private JBList<String> featuresList;
    private DefaultListModel<String> featuresListModel;

    // Partition list (multi-select)
    private JBList<String> partitionsList;
    private DefaultListModel<String> partitionsListModel;

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

        partitionsListModel = new DefaultListModel<>();
        partitionsList = new JBList<>(partitionsListModel);
        partitionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (partitionScrollPane != null) {
            partitionScrollPane.setViewportView(partitionsList);
        }

        DBNInfoLabel partitionInfoLabel = new DBNInfoLabel();
        partitionInfoLabel.setContent(html(this, "info/partition_model_info.html.ft"));
        partitionLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        partitionLabelPanel.setOpaque(false);
        partitionLabelPanel.add(new JLabel("Partition Columns"));
        partitionLabelPanel.add(partitionInfoLabel);

        updatePartitionVisibility();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(
                () -> !availableColumns.isEmpty(),
                array(featuresScrollPane, labelComboBox));
    }

    @Override
    protected void initEventListeners() {
        partitionEnabledCheckBox.addActionListener(e -> updatePartitionVisibility());
    }

    private void updatePartitionVisibility() {
        partitionScrollPane.setVisible(partitionEnabledCheckBox.isSelected());
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(featuresLabel, featuresScrollPane);
        alignerData.registerFieldGroup(labelLabel, labelComboBox);
        alignerData.registerFieldGroup(partitionLabelPanel, partitionEnabledCheckBox);
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
        } else if (sourceType == MLSourceType.OBJECT_STORAGE) {
            loadColumnsFromCloud(sourceForm);
        }
    }

    private void showLoadingState() {
        JLabel loadingLabel = new JLabel("Loading...", SwingConstants.CENTER);
        loadingLabel.setEnabled(false);
        featuresScrollPane.setViewportView(loadingLabel);
        labelComboBox.setEnabled(false);
    }

    private void showLoadedState() {
        featuresScrollPane.setViewportView(featuresList);
        labelComboBox.setEnabled(true);
    }

    /**
     * Loads column names from CSV file headers.
     * Reads file path directly from source form (not config) because config may not be updated yet.
     */
    private void loadColumnsFromCSV(MLSourceForm sourceForm) {
        // Get file path directly from the source form's file field
        String filePath = sourceForm.getSelectedFilePath();

        if (filePath == null || filePath.isEmpty()) {
            clearColumns();
            return;
        }

        // Get delimiter directly from source form
        String delimiter = sourceForm.getSelectedDelimiter();

        showLoadingState();

        // Read CSV headers in background
        Background.run(() -> {
            try {
                List<String> headers = readCSVHeaders(filePath, delimiter);

                // Update UI on EDT
                Dispatch.run(featuresScrollPane, () -> {
                    updateColumnsUI(headers);
                });
            } catch (Exception e) {
                log.warn("Failed to read CSV headers", e);
                Dispatch.run(featuresScrollPane, this::clearColumns);
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

        showLoadingState();

        // Load columns in background
        Background.run(() -> {
            List<DBColumn> columns = table.getColumns();
            List<String> columnNames = new ArrayList<>(columns.size());
            for (DBColumn column : columns) {
                columnNames.add(column.getName());
            }

            // Update UI on EDT
            Dispatch.run(featuresScrollPane, () -> {
                updateColumnsUI(columnNames);
            });
        });
    }

    /**
     * Loads column names from cloud source (already cached by MLSourceCloudForm).
     */
    private void loadColumnsFromCloud(MLSourceForm sourceForm) {
        List<String> columns = sourceForm.getCloudDiscoveredColumns();
        if (columns == null || columns.isEmpty()) {
            clearColumns();
            return;
        }
        updateColumnsUI(columns);
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
        showLoadedState();
        this.availableColumns = new ArrayList<>(columns);

        // Update features list
        featuresListModel.clear();
        for (String column : columns) {
            featuresListModel.addElement(column);
        }

        // Update label combo box
        ComboBoxes.initComboBox(labelComboBox, columns);

        // Update partition list
        partitionsListModel.clear();
        for (String column : columns) {
            partitionsListModel.addElement(column);
        }

        updateFieldAvailability();

        // Restore saved selections if any
        MLFeatureConfig config = getConfig();
        restoreSelections(config);
    }

    private void clearColumns() {
        showLoadedState();
        availableColumns.clear();
        featuresListModel.clear();
        partitionsListModel.clear();
        ComboBoxes.initComboBox(labelComboBox, Collections.emptyList());
        updateFieldAvailability();
    }

    private void restoreSelections(MLFeatureConfig config) {
        restoreListSelections(featuresList, featuresListModel, config.getFeatureColumns());

        String savedLabel1 = config.getLabelColumn();
        if (savedLabel1 != null && availableColumns.contains(savedLabel1)) {
            ComboBoxes.setSelection(labelComboBox, savedLabel1);
        }

        restoreListSelections(partitionsList, partitionsListModel, getTrainerConfig().getPartitionColumns());
    }

    private void restoreListSelections(JBList<String> list, DefaultListModel<String> model, List<String> saved) {
        if (saved == null || saved.isEmpty()) return;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            if (saved.contains(model.get(i))) indices.add(i);
        }
        if (!indices.isEmpty()) {
            list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public List<String> getSelectedFeatures() {
        if (featuresList == null) return new ArrayList<>();
        return new ArrayList<>(featuresList.getSelectedValuesList());
    }

    public String getSelectedLabel() {
        return ComboBoxes.getSelection(labelComboBox);
    }

    public List<String> getSelectedPartitionColumns() {
        if (partitionsList == null) return new ArrayList<>();
        return new ArrayList<>(partitionsList.getSelectedValuesList());
    }

    private MLFeatureConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLFeatureConfig();
        return toolboxForm.getMLRequest().getFeatureConfig();
    }

    private MLTrainerConfig getTrainerConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLTrainerConfig();
        return toolboxForm.getMLRequest().getTrainerConfig();
    }

    @Override
    public void resetFormChanges() {
        if (featuresListModel == null) return;
        MLTrainerConfig trainerConfig = getTrainerConfig();
        partitionEnabledCheckBox.setSelected(trainerConfig.isPartitioned());
        updatePartitionVisibility();
        refreshColumns();
    }

    @Override
    public void applyFormChanges() {
        MLFeatureConfig config = getConfig();
        config.setFeatureColumns(getSelectedFeatures());
        config.setLabelColumn(getSelectedLabel());

        MLTrainerConfig trainerConfig = getTrainerConfig();
        trainerConfig.setPartitioned(partitionEnabledCheckBox.isSelected());
        trainerConfig.setPartitionColumns(partitionEnabledCheckBox.isSelected() ? getSelectedPartitionColumns() : new ArrayList<>());
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
        String label = getSelectedLabel();
        if (features.isEmpty() && (label == null || label.isEmpty())) {
            return null;
        }
        String labelInfo = (label == null || label.isEmpty()) ? "none" : label;
        return features.size() + " features, label: " + labelInfo;
    }
}
