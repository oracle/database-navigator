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

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.dialog.SelectionListCellRenderer;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.select.DBNComboBoxRenderer;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Form for selecting features and labels for ML training.
 * Supports database tables, local CSV files and object-storage CSV files.
 *
 * For CSV: reads headers from file and displays as column names
 * For DB: loads columns from selected table
 */
@Slf4j
public class MLFeatureForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private static final ListCellRenderer<Presentable> COLUMN_LIST_CELL_RENDERER = new SelectionListCellRenderer<>();

    private JPanel mainPanel;
    private JLabel featuresLabel;
    private JBScrollPane featuresScrollPane;
    private JLabel featureSelectionCountLabel;
    private JLabel labelLabel;
    private DBNComboBox<Presentable> labelComboBox;
    private JPanel partitionLabelPanel;
    private DBNInfoLabel partitionInfoLabel;
    private JCheckBox partitionEnabledCheckBox;
    private JBScrollPane partitionScrollPane;

    // Features list (multi-select)
    private JBList<Presentable> featuresList;
    private DefaultListModel<Presentable> featuresListModel;

    // Partition list (multi-select)
    private JBList<Presentable> partitionsList;
    private DefaultListModel<Presentable> partitionsListModel;

    private HyperlinkLabel selectAllFeaturesLink;
    private HyperlinkLabel clearFeaturesLink;

    private boolean restoreConfiguredSelection;

    public MLFeatureForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    private void initComponents() {
        featuresListModel = new DefaultListModel<>();
        featuresList = new JBList<>(featuresListModel);
        featuresList.setCellRenderer(COLUMN_LIST_CELL_RENDERER);
        featuresList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        featuresList.setVisibleRowCount(6);
        if (featuresScrollPane != null) {
            featuresScrollPane.setViewportView(featuresList);
        }

        partitionsListModel = new DefaultListModel<>();
        partitionsList = new JBList<>(partitionsListModel);
        partitionsList.setCellRenderer(COLUMN_LIST_CELL_RENDERER);
        partitionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        if (partitionScrollPane != null) {
            partitionScrollPane.setViewportView(partitionsList);
        }

        partitionInfoLabel.setContent(html(this, "info/partition_model_info.html.ft"));

        initLabelComboBox();
        initFeatureSelectionLinks();
        featuresLabel.setLabelFor(featuresList);
        labelLabel.setLabelFor(labelComboBox);
        updateFeatureSelectionState();
        updatePartitionVisibility();
    }

    private void initFeatureSelectionLinks() {
        initHyperlink(
                selectAllFeaturesLink,
                txt("app.shared.action.SelectAll"),
                this::selectAllFeatures);
        initHyperlink(
                clearFeaturesLink,
                txt("cfg.machineLearning.action.ClearSelection"),
                featuresList::clearSelection);
    }

    @Override
    protected void initValidation() {
        addValidation(featuresList,
                list -> !list.isSelectionEmpty(),
                txt("msg.machineLearning.error.SelectFeature"));
        addValidation(labelComboBox, this::validateTargetColumn);
        addValidation(partitionsList,
                list -> !partitionEnabledCheckBox.isSelected() || !list.isSelectionEmpty(),
                txt("msg.machineLearning.error.SelectPartitionColumn"));
    }

    private @DialogMessage String validateTargetColumn(DBNComboBox<Presentable> comboBox) {
        Presentable selection = ComboBoxes.getSelection(comboBox);
        String targetColumn = selection == null ? null : selection.getName();
        if (targetColumn == null) return txt("msg.machineLearning.error.SelectTargetColumn");
        if (getSelectedFeatures().contains(targetColumn)) {
            return txt("msg.machineLearning.error.TargetCannotBeFeature");
        }
        return null;
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(
                () -> !featuresListModel.isEmpty(),
                array(featuresScrollPane, labelComboBox, partitionScrollPane));
    }

    @Override
    protected void initEventListeners() {
        featuresList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;

            updateFeatureSelectionState();
            validateInput(featuresList);
            validateInput(labelComboBox);
        });
        partitionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) validateInput(partitionsList);
        });
        onSelectionChange(labelComboBox, value -> updateFeatureSelectionState());
        partitionEnabledCheckBox.addActionListener(e -> {
            updatePartitionVisibility();
            validateInput(partitionsList);
        });
    }

    private void selectAllFeatures() {
        String targetColumn = getSelectedLabel();
        if (targetColumn == null) return;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < featuresListModel.size(); i++) {
            if (!featuresListModel.get(i).getName().equals(targetColumn)) indices.add(i);
        }
        featuresList.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
    }

    private void updateFeatureSelectionState() {
        int selectedCount = featuresList.getSelectedIndices().length;
        featureSelectionCountLabel.setText(txt(
                "cfg.machineLearning.text.FeatureSelectionCount",
                selectedCount));
        selectAllFeaturesLink.setEnabled(hasUnselectedFeature());
        clearFeaturesLink.setEnabled(selectedCount > 0);
    }

    private boolean hasUnselectedFeature() {
        String targetColumn = getSelectedLabel();
        if (targetColumn == null) return false;

        for (int i = 0; i < featuresListModel.size(); i++) {
            Presentable column = featuresListModel.get(i);
            if (!column.getName().equals(targetColumn) && !featuresList.isSelectedIndex(i)) return true;
        }
        return false;
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

    /** Reloads dependent columns, preserving matching selections for passive source reloads. */
    private void refreshColumns(boolean preserveSelection) {
        MLSourceForm sourceForm = getSourceForm();
        boolean restoreSelection = restoreConfiguredSelection &&
                sourceForm.isConfiguredSourceSelected();

        if (restoreSelection) {
            armTargetPreselector(getConfig().getLabelColumn());
        } else if (!preserveSelection) {
            armTargetPreselector(null);
        }

        labelComboBox.withValueLoader(createColumnLoader(sourceForm));
        if (preserveSelection) {
            labelComboBox.reloadValues();
            validateFormFields();
        } else {
            clearColumns();
            labelComboBox.triggerLoad();
        }
    }

    private Supplier<List<Presentable>> createColumnLoader(MLSourceForm sourceForm) {
        MLSourceType sourceType = sourceForm.getSelectedSourceType();
        if (sourceType == null) return Collections::emptyList;

        if (sourceType == MLSourceType.FILE_SYSTEM) {
            String filePath = sourceForm.getSelectedFilePath();
            String delimiter = sourceForm.getSelectedFileDelimiter();
            return () -> loadFileColumns(filePath, delimiter);
        }

        if (sourceType == MLSourceType.DATABASE_TABLE) {
            DBTable table = sourceForm.getSelectedTable();
            return () -> table == null ?
                    Collections.emptyList() :
                    toColumnOptions(Lists.convert(table.getColumns(), DBColumn::getName));
        }

        if (sourceType == MLSourceType.OBJECT_STORAGE) {
            List<String> columns = sourceForm.getCloudDiscoveredColumns();
            return () -> toColumnOptions(columns);
        }

        return Collections::emptyList;
    }

    private List<Presentable> loadFileColumns(String filePath, String delimiter) {
        try {
            return toColumnOptions(readCSVHeaders(filePath, delimiter));
        } catch (IOException e) {
            conditionallyLog(e);
            log.warn("Failed to load source columns", e);
            return Collections.emptyList();
        }
    }

    private List<Presentable> toColumnOptions(List<String> columns) {
        return Lists.convert(columns, column -> Presentable.basic(column, Icons.DBO_COLUMN));
    }

    private List<String> readCSVHeaders(String filePath, String delimiter) throws IOException {
        if (filePath == null || filePath.isBlank()) return Collections.emptyList();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isEmpty()) {
                return Collections.emptyList();
            }

            String[] headers = headerLine.split(Pattern.quote(delimiter));
            List<String> result = new ArrayList<>(headers.length);
            for (String header : headers) {
                result.add(header.trim());
            }
            return result;
        }
    }

    private void clearColumns() {
        labelComboBox.clearValues();
        featuresListModel.clear();
        partitionsListModel.clear();
        updateFieldAvailability();
        updateFeatureSelectionState();
        validateFormFields();
    }

    /**
     * Applies a completed column load. Invoked on the dispatch thread by the combo box loader,
     * only for the load that is still current and after the target selection has been applied.
     */
    private void onColumnsLoaded(List<Presentable> columns) {
        boolean restoreSelection = restoreConfiguredSelection && isConfiguredSourceSelected();
        List<String> selectedFeatures = restoreSelection ?
                getConfig().getFeatureColumns() :
                getSelectedFeatures();
        List<String> selectedPartitions = restoreSelection ?
                getTrainerConfig().getPartitionColumns() :
                getSelectedPartitionColumns();

        featuresListModel.clear();
        partitionsListModel.clear();
        featuresListModel.addAll(columns);
        partitionsListModel.addAll(columns);

        updateFieldAvailability();
        restoreListSelections(featuresList, featuresListModel, selectedFeatures);
        restoreListSelections(partitionsList, partitionsListModel, selectedPartitions);

        if (restoreSelection && !columns.isEmpty()) {
            restoreConfiguredSelection = false;
            armTargetPreselector(null);
        }

        updateFeatureSelectionState();
        validateFormFields();
    }

    private boolean isConfiguredSourceSelected() {
        return getSourceForm().isConfiguredSourceSelected();
    }

    private MLSourceForm getSourceForm() {
        return getToolboxForm().getSourceForm();
    }

    public @DialogMessage String validateColumnSelection() {
        if (mainPanel.isShowing() &&
                featuresList.isEnabled() &&
                labelComboBox.isEnabled() &&
                (!partitionEnabledCheckBox.isSelected() || partitionsList.isEnabled())) {
            return null;
        }

        if (getSelectedFeatures().isEmpty()) {
            return txt("msg.machineLearning.error.SelectFeature");
        }

        String targetValidation = validateTargetColumn(labelComboBox);
        if (targetValidation != null) return targetValidation;

        if (partitionEnabledCheckBox.isSelected() && getSelectedPartitionColumns().isEmpty()) {
            return txt("msg.machineLearning.error.SelectPartitionColumn");
        }

        return null;
    }

    private void initLabelComboBox() {
        labelComboBox.setRenderer(createTargetColumnCellRenderer());
        labelComboBox.withValueLoadConsumer(this::onColumnsLoaded);
    }

    private ListCellRenderer<Presentable> createTargetColumnCellRenderer() {
        return new DBNComboBoxRenderer<>(labelComboBox) {
            @Override
            protected void customize(@NotNull JList<? extends Presentable> list,
                                     Presentable value,
                                     int index,
                                     boolean selected,
                                     boolean hasFocus) {
                super.customize(list, value, index, selected, hasFocus);
                if (value != null && index == -1) setIcon(Icons.ML_TARGET_COLUMN);
            }
        };
    }

    private void restoreListSelections(JBList<Presentable> list, DefaultListModel<Presentable> model, List<String> saved) {
        list.clearSelection();
        if (saved == null || saved.isEmpty()) return;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            if (saved.contains(model.get(i).getName())) indices.add(i);
        }
        if (!indices.isEmpty()) {
            list.setSelectedIndices(indices.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public List<String> getSelectedFeatures() {
        if (featuresList == null) return new ArrayList<>();
        return Lists.convert(featuresList.getSelectedValuesList(), Presentable::getName);
    }

    public String getSelectedLabel() {
        Presentable selection = ComboBoxes.getSelection(labelComboBox);
        return selection == null ? null : selection.getName();
    }

    public List<String> getSelectedPartitionColumns() {
        if (partitionsList == null) return new ArrayList<>();
        return Lists.convert(partitionsList.getSelectedValuesList(), Presentable::getName);
    }

    private MLFeatureConfig getConfig() {
        return getMLRequest().getFeatureConfig();
    }

    private MLTrainerConfig getTrainerConfig() {
        return getMLRequest().getTrainerConfig();
    }

    @Override
    public void resetFormChanges() {
        if (featuresListModel == null) return;
        MLTrainerConfig trainerConfig = getTrainerConfig();
        partitionEnabledCheckBox.setSelected(trainerConfig.isPartitioned());
        updatePartitionVisibility();

        restoreConfiguredSelection = true;
        refreshColumns(false);
    }

    public void sourceChanged() {
        restoreConfiguredSelection = false;
        refreshColumns(false);
    }

    public void sourceInvalidated() {
        restoreConfiguredSelection = false;
        armTargetPreselector(null);
        labelComboBox.withValueLoader(Collections::emptyList);
        clearColumns();
        labelComboBox.triggerLoad();
    }

    public void sourceLoaded() {
        refreshColumns(true);
    }

    /**
     * Arms a one-time preselection of the configured target column. Cleared on a genuine source
     * change, so a target that never matched cannot be applied to an unrelated source later on.
     */
    private void armTargetPreselector(String targetColumn) {
        labelComboBox.withValuePreselector(
                targetColumn == null || targetColumn.isBlank() ?
                        null :
                        column -> targetColumn.equals(column.getName()));
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
        return txt("cfg.machineLearning.title.FeaturesAndLabel");
    }

    @Override
    public String getFormTitleDetail() {
        List<String> features = getSelectedFeatures();
        String label = getSelectedLabel();
        if (features.isEmpty() && (label == null || label.isEmpty())) {
            return null;
        }
        String labelInfo = (label == null || label.isEmpty()) ? txt("cfg.machineLearning.placeholder.None") : label;
        return txt("cfg.machineLearning.text.FeatureSelectionDetail", features.size(), labelInfo);
    }
}
