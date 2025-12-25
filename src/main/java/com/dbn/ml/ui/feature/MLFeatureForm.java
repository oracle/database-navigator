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
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.object.type.DBObjectType.COLUMN;

public class MLFeatureForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel featuresLabel;
    private JBScrollPane featuresScrollPane;
    private JLabel labelLabel;
    private DBObjectSelector<DBColumn> labelComboBox;
    
    // Features list (multi-select)
    private JBList<DBColumn> featuresList;
    private DefaultListModel<DBColumn> featuresListModel;

    public MLFeatureForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    private void initComponents() {
        featuresListModel = new DefaultListModel<>();
        featuresList = new JBList<>(featuresListModel);
        featuresList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Use column name for display
        featuresList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value != null ? value.getName() : "");
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            }
            return label;
        });
        
        if (featuresScrollPane != null) {
            featuresScrollPane.setViewportView(featuresList);
        }
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        // Features and label only available when table is selected
        fieldAdapter.initFieldsAvailability(
                () -> isValid(getSelectedTable()), 
                array(featuresScrollPane, labelComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(featuresLabel, featuresScrollPane);
        alignerData.registerFieldGroup(labelLabel, labelComboBox);
    }

    private void initComboBoxes() {
        MLFeatureConfig config = getConfig();

        // Label column selector - follows VectorToolbox pattern
        labelComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadAllColumns())
                .withValuePreselector(() -> config.getLabelColumn())
                .triggerLoad();

        // Load features list in background (same pattern as DBObjectSelector)
        loadFeaturesListAsync();
        
        updateFieldAvailability();
    }

    /**
     * Loads all columns from the selected table.
     * Called in background thread by DBObjectSelector.
     */
    private List<DBColumn> loadAllColumns() {
        DBTable table = getSelectedTable();
        if (table == null) return Collections.emptyList();
        
        // This triggers lazy loading of columns
        return table.getColumns();
    }

    /**
     * Loads columns into the features list asynchronously.
     * Uses Background.run() to load columns, then Dispatch.run() to update UI.
     */
    private void loadFeaturesListAsync() {
        System.out.println("=== loadFeaturesListAsync() called ===");
        featuresListModel.clear();
        
        DBTable table = getSelectedTable();
        System.out.println("Selected table: " + table);
        if (table == null) return;
        
        // Load columns in background thread
        Background.run(() -> {
            System.out.println("Background thread started for table: " + table.getName());
            // This triggers lazy loading
            List<DBColumn> columns = table.getColumns();
            System.out.println("Loaded " + columns.size() + " columns in background");
            
            // Update UI on EDT
            Dispatch.run(featuresList, () -> {
                System.out.println("Dispatch.run() - updating UI with " + columns.size() + " columns");
                featuresListModel.clear();
                for (DBColumn column : columns) {
                    featuresListModel.addElement(column);
                    System.out.println("  Added column: " + column.getName());
                }
                System.out.println("featuresListModel size: " + featuresListModel.size());
            });
        });
    }

    /**
     * Called when source table changes - reload columns.
     */
    public void refreshColumns() {
        labelComboBox.reloadValues();
        loadFeaturesListAsync();
        updateFieldAvailability();
    }

    private DBTable getSelectedTable() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return null;
        
        MLSourceForm sourceForm = toolboxForm.getSourceForm();
        if (sourceForm == null) return null;
        
        return sourceForm.getSelectedTable();
    }

    public List<String> getSelectedFeatures() {
        if (featuresList == null) return new ArrayList<>();
        
        List<DBColumn> selectedColumns = featuresList.getSelectedValuesList();
        List<String> featureNames = new ArrayList<>(selectedColumns.size());
        for (DBColumn column : selectedColumns) {
            featureNames.add(column.getName());
        }
        return featureNames;
    }

    public String getSelectedLabel() {
        DBColumn column = ComboBoxes.getSelection(labelComboBox);
        return column != null ? column.getName() : null;
    }

    private MLFeatureConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLFeatureConfig();
        return toolboxForm.getMLRequest().getFeatureConfig();
    }

    @Override
    public void resetFormChanges() {
        if (featuresListModel == null) return;
        
        initComboBoxes();
        
        // Restore saved feature selections after a short delay to let columns load
        MLFeatureConfig config = getConfig();
        List<String> savedFeatures = config.getFeatureColumns();
        if (savedFeatures != null && !savedFeatures.isEmpty()) {
            // Delay selection restoration to allow async loading to complete
            Dispatch.run(() -> restoreFeatureSelections(savedFeatures));
        }
    }

    private void restoreFeatureSelections(List<String> savedFeatures) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < featuresListModel.size(); i++) {
            DBColumn column = featuresListModel.get(i);
            if (savedFeatures.contains(column.getName())) {
                indices.add(i);
            }
        }
        if (!indices.isEmpty()) {
            int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();
            featuresList.setSelectedIndices(indicesArray);
        }
    }

    @Override
    public void applyFormChanges() {
        MLFeatureConfig config = getConfig();
        config.setFeatureColumns(getSelectedFeatures());
        config.setLabelColumn(getSelectedLabel());
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
        if (features.isEmpty() && label == null) {
            return null;
        }
        return features.size() + " features, label: " + (label != null ? label : "none");
    }
}
