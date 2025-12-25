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
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class MLTrainerForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel algorithmLabel;
    private DBNComboBox<MLTrainerType> algorithmComboBox;
    private JTextArea descriptionTextArea;
    private JLabel splitLabel;
    private JSlider splitSlider;
    private JLabel splitValueLabel;
    private JCheckBox useFixedSeedCheckBox;
    private JLabel seedLabel;
    private JSpinner seedSpinner;

    public MLTrainerForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(algorithmLabel, algorithmComboBox);
        alignerData.registerFieldGroup(splitLabel, splitSlider);
        alignerData.registerFieldGroup(seedLabel, seedSpinner);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(algorithmComboBox, t -> onAlgorithmChanged());
        if (splitSlider != null) {
            splitSlider.addChangeListener(e -> updateSplitLabel());
        }
        if (useFixedSeedCheckBox != null) {
            useFixedSeedCheckBox.addActionListener(e -> updateSeedEnabled());
        }
    }

    private void initComponents() {
        if (algorithmComboBox == null) return;
        
        // Algorithm combo
        algorithmComboBox.setValues(MLTrainerType.values());
        algorithmComboBox.setSelectedValue(MLTrainerType.SVM_LINEAR);

        // Split slider
        if (splitSlider != null) {
            splitSlider.setMinimum(10);
            splitSlider.setMaximum(90);
            splitSlider.setValue(70);
        }

        // Seed spinner
        if (seedSpinner != null) {
            seedSpinner.setModel(new SpinnerNumberModel(1L, 0L, Long.MAX_VALUE, 1L));
        }

        onAlgorithmChanged();
        updateSplitLabel();
        updateSeedEnabled();
    }

    private void onAlgorithmChanged() {
        if (algorithmComboBox == null || descriptionTextArea == null) return;
        
        MLTrainerType trainerType = algorithmComboBox.getSelectedValue();
        if (trainerType != null) {
            descriptionTextArea.setText(trainerType.getDescription());
        }
    }

    private void updateSplitLabel() {
        if (splitSlider == null || splitValueLabel == null) return;
        
        int trainPercent = splitSlider.getValue();
        int testPercent = 100 - trainPercent;
        splitValueLabel.setText(String.format("%d%% Train / %d%% Test", trainPercent, testPercent));
    }

    private void updateSeedEnabled() {
        if (useFixedSeedCheckBox == null) return;
        
        boolean useFixed = useFixedSeedCheckBox.isSelected();
        if (seedLabel != null) seedLabel.setEnabled(useFixed);
        if (seedSpinner != null) seedSpinner.setEnabled(useFixed);
    }

    @Override
    public void resetFormChanges() {
        if (algorithmComboBox == null) return;
        
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return;
        
        MLTrainerConfig config = toolboxForm.getMLRequest().getTrainerConfig();

        MLTrainerType trainerType = config.getTrainerType();
        if (trainerType == null) trainerType = MLTrainerType.SVM_LINEAR;
        algorithmComboBox.setSelectedValue(trainerType);
        
        if (splitSlider != null) {
            int splitPercent = (int) (config.getTrainTestSplitRatio() * 100);
            splitSlider.setValue(splitPercent);
        }
        
        if (useFixedSeedCheckBox != null) {
            useFixedSeedCheckBox.setSelected(config.isUseFixedSeed());
        }
        
        if (seedSpinner != null) {
            seedSpinner.setValue(config.getRandomSeed());
        }
        
        updateSeedEnabled();
        onAlgorithmChanged();
        updateSplitLabel();
    }

    @Override
    public void applyFormChanges() {
        if (algorithmComboBox == null) return;
        
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return;
        
        MLTrainerConfig config = toolboxForm.getMLRequest().getTrainerConfig();

        config.setTrainerType(algorithmComboBox.getSelectedValue());
        
        if (splitSlider != null) {
            config.setTrainTestSplitRatio(splitSlider.getValue() / 100.0);
        }
        
        if (useFixedSeedCheckBox != null) {
            config.setUseFixedSeed(useFixedSeedCheckBox.isSelected());
        }
        
        if (seedSpinner != null) {
            config.setRandomSeed((Long) seedSpinner.getValue());
        }
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
        if (algorithmComboBox == null || splitSlider == null) return null;
        
        MLTrainerType trainerType = algorithmComboBox.getSelectedValue();
        if (trainerType == null) return null;
        
        int trainPercent = splitSlider.getValue();
        return trainerType.getName() + ", " + trainPercent + "/" + (100 - trainPercent) + " split";
    }
}
