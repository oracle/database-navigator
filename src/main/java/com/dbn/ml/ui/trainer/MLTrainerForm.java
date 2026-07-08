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

package com.dbn.ml.ui.trainer;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.MLMiningFunction;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class MLTrainerForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel modelNameLabel;
    private JBTextField modelNameField;
    private JLabel algorithmLabel;
    private DBNComboBox<MLTrainerType> algorithmComboBox;
    private JPanel algorithmLinkPanel;
    private JLabel splitLabel;
    private JSlider splitSlider;
    private JLabel splitValueLabel;
    private JCheckBox useFixedSeedCheckBox;
    private JLabel seedLabel;
    private JSpinner seedSpinner;

    private final HyperlinkLabel algorithmDocLink = new HyperlinkLabel("Oracle Documentation");

    public MLTrainerForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    private void initComponents() {
        modelNameField.getEmptyText().setText("Auto-generated if empty");
        splitSlider.setMinimum(10);
        splitSlider.setMaximum(90);
        seedSpinner.setModel(new SpinnerNumberModel(1L, 0L, Long.MAX_VALUE, 1L));

        algorithmLinkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        algorithmLinkPanel.setOpaque(false);
        algorithmLinkPanel.add(algorithmDocLink);

    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(modelNameLabel, modelNameField);
        alignerData.registerFieldGroup(algorithmLabel, algorithmComboBox);
        alignerData.registerFieldGroup(splitLabel, splitSlider);
        alignerData.registerFieldGroup(seedLabel, seedSpinner);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(algorithmComboBox, t -> onAlgorithmChanged());
        splitSlider.addChangeListener(e -> updateSplitLabel());
        useFixedSeedCheckBox.addActionListener(e -> updateSeedEnabled());
    }

    public void refreshTrainers(MLMiningFunction miningFunction) {
        MLTaskType taskType = miningFunction != null ? miningFunction.getTaskType() : null;
        List<MLTrainerType> availableTrainers = MLTrainerType.getTrainersForTask(taskType);

        MLTrainerType currentSelection = algorithmComboBox.getSelectedValue();
        algorithmComboBox.setValues(availableTrainers.toArray(new MLTrainerType[0]));

        if (availableTrainers.contains(currentSelection)) {
            algorithmComboBox.setSelectedValue(currentSelection);
        } else if (!availableTrainers.isEmpty()) {
            algorithmComboBox.setSelectedValue(availableTrainers.get(0));
        }

        onAlgorithmChanged();
    }

    private void onAlgorithmChanged() {
        MLTrainerType trainerType = algorithmComboBox.getSelectedValue();
        if (trainerType != null) {
            algorithmDocLink.setHyperlinkTarget(trainerType.getDocUrl());
        }
    }

    private void updateSplitLabel() {
        int trainPercent = splitSlider.getValue();
        int testPercent = 100 - trainPercent;
        splitValueLabel.setText(String.format("%d%% Train / %d%% Test", trainPercent, testPercent));
    }

    private void updateSeedEnabled() {
        boolean useFixed = useFixedSeedCheckBox.isSelected();
        seedLabel.setEnabled(useFixed);
        seedSpinner.setEnabled(useFixed);
    }

    private MLTrainerConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLTrainerConfig();
        return toolboxForm.getMLRequest().getTrainerConfig();
    }

    @Override
    public void resetFormChanges() {
        MLTrainerConfig config = getConfig();

        modelNameField.setText(config.getModelName() != null ? config.getModelName() : "");

        MLTrainerType trainerType = config.getTrainerType();
        if (trainerType == null) trainerType = MLTrainerType.SVM_CLASSIFICATION;

        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        MLMiningFunction miningFunction = toolboxForm != null ? toolboxForm.getMLRequest().getMiningFunction() : MLMiningFunction.CLASSIFICATION;
        refreshTrainers(miningFunction);
        algorithmComboBox.setSelectedValue(trainerType);

        splitSlider.setValue((int) (config.getTrainTestSplitRatio() * 100));
        useFixedSeedCheckBox.setSelected(config.isUseFixedSeed());
        seedSpinner.setValue(config.getRandomSeed());

        updateSplitLabel();
        updateSeedEnabled();
    }

    @Override
    public void applyFormChanges() {
        MLTrainerConfig config = getConfig();
        String name = modelNameField.getText().trim();
        config.setModelName(name.isEmpty() ? null : name);
        config.setTrainerType(algorithmComboBox.getSelectedValue());
        config.setTrainTestSplitRatio(splitSlider.getValue() / 100.0);
        config.setUseFixedSeed(useFixedSeedCheckBox.isSelected());
        // spinner editor may commit typed input as Double - coerce through Number
        config.setRandomSeed(((Number) seedSpinner.getValue()).longValue());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Training Configuration";
    }

    @Override
    public String getFormTitleDetail() {
        MLTrainerType trainerType = algorithmComboBox.getSelectedValue();
        if (trainerType == null) return null;

        int trainPercent = splitSlider.getValue();
        return trainerType.getName() + ", " + trainPercent + "/" + (100 - trainPercent) + " split";
    }
}
