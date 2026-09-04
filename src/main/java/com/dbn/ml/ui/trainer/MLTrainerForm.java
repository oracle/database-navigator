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

import com.dbn.common.Priority;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Naming;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.model.MLMiningFunction;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBTextField;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.Focus.onFocusLost;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.nls.NlsResources.txt;

public class MLTrainerForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel modelNameLabel;
    private JBTextField modelNameField;
    private JLabel algorithmLabel;
    private DBNComboBox<MLTrainerType> algorithmComboBox;
    private HyperlinkLabel algorithmDocLink;
    private JLabel splitLabel;
    private JSlider splitSlider;
    private JLabel splitValueLabel;
    private JCheckBox useFixedSeedCheckBox;
    private JLabel seedLabel;
    private JSpinner seedSpinner;

    // Loaded once in the background - null until the load completes, at which point the
    // model name field is re-validated. Empty result and "not loaded yet" are indistinguishable
    // on purpose: while unknown, a typed name is assumed available rather than blocking Train.
    private volatile Set<String> existingModelNames;
    private String generatedModelName;
    private String generatedSourceName;

    public MLTrainerForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
        loadExistingModelNames();
    }

    private void initComponents() {
        splitSlider.setMinimum(10);
        splitSlider.setMaximum(90);
        seedSpinner.setModel(new SpinnerNumberModel(1L, 0L, MLTrainerConfig.MAX_RANDOM_SEED, 1L));
        setPreferredWidth(splitSlider, 200);
        setPreferredWidth(seedSpinner, 200);
    }

    private static void setPreferredWidth(JComponent component, int width) {
        Dimension size = component.getPreferredSize();
        component.setPreferredSize(new Dimension(width, size.height));
    }

    /**
     * Fetches the model names already present in the schema, so the name field can warn about a
     * collision before the training job is submitted instead of after it fails on the server.
     */
    private void loadExistingModelNames() {
        Background.run(() -> {
            Set<String> names = new HashSet<>();
            ConnectionHandler connection = getConnection();
            DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();

            DatabaseInterfaceInvoker.execute(Priority.LOW,
                    connection.getProject(),
                    getConnectionId(),
                    (DBNConnection conn) -> {
                        try (ResultSet rs = mlInterface.getExistingModelNames(conn)) {
                            while (rs.next()) {
                                names.add(rs.getString("MODEL_NAME").toUpperCase());
                            }
                        }
                    });

            existingModelNames = names;
            Dispatch.run(() -> {
                updateGeneratedModelName(generatedSourceName);
                validateInput(modelNameField);
            });
        });
    }

    @Override
    protected void initValidation() {
        addTextValidation(modelNameField, this::validateModelName);
    }

    private @DialogMessage String validateModelName(JTextComponent field) {
        String name = getText(field).trim();
        if (name.isEmpty()) return null; // blank means auto-generate, always valid

        String upperName = name.toUpperCase();
        if (!Strings.isAlphanumericWithUnderscore(upperName)) return txt("msg.machineLearning.error.ModelNameInvalid");

        Set<String> names = existingModelNames;
        if (names != null && names.contains(upperName)) return txt("msg.machineLearning.error.ModelNameExists");

        return null;
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(modelNameLabel, modelNameField);
        alignerData.registerFieldGroup(algorithmLabel, algorithmComboBox);
        alignerData.registerFieldGroup(splitLabel);
        alignerData.registerFieldGroup(seedLabel);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(algorithmComboBox, t -> onAlgorithmChanged());
        splitSlider.addChangeListener(e -> updateSplitLabel());
        useFixedSeedCheckBox.addActionListener(e -> updateSeedEnabled());
        onFocusLost(modelNameField, e -> {
            if (getText(modelNameField).isBlank()) updateGeneratedModelName(generatedSourceName);
        });
    }

    public void refreshTrainers(MLMiningFunction miningFunction) {
        MLTaskType taskType = miningFunction != null ? miningFunction.getTaskType() : null;
        List<MLTrainerType> availableTrainers = MLTrainerType.getTrainersForTask(
                taskType, getConnection().getDatabaseVersion());

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
            initHyperlink(
                    algorithmDocLink,
                    txt("cfg.machineLearning.link.OracleDocumentation"),
                    trainerType.getDocUrl());
        }
    }

    private void updateSplitLabel() {
        int trainPercent = splitSlider.getValue();
        int testPercent = 100 - trainPercent;
        splitValueLabel.setText(txt(
                "cfg.machineLearning.label.TrainTestSplitValue",
                trainPercent,
                testPercent));
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

    public void refreshGeneratedModelName() {
        updateGeneratedModelName(getToolboxForm().getSourceForm().getSelectedSourceBaseName());
    }

    public void invalidateGeneratedModelName() {
        generatedSourceName = null;
        String currentName = getText(modelNameField).trim();
        if (!currentName.isEmpty() && !currentName.equals(generatedModelName)) return;

        generatedModelName = null;
        setTextSilently(modelNameField, "");
        validateInput(modelNameField);
    }

    private void updateGeneratedModelName(String sourceName) {
        generatedSourceName = sourceName;
        String currentName = getText(modelNameField).trim();
        if (!currentName.isEmpty() && !currentName.equals(generatedModelName)) return;

        String baseName = MLSourceNames.getModelBaseName(sourceName);
        Set<String> names = existingModelNames;
        generatedModelName = baseName == null ? null :
                Naming.nextNumberedIdentifier(baseName, false, name -> names != null && names.contains(name));

        setTextSilently(modelNameField, generatedModelName == null ? "" : generatedModelName);
        validateInput(modelNameField);
    }

    @Override
    public void resetFormChanges() {
        MLTrainerConfig config = getConfig();

        generatedModelName = null;
        generatedSourceName = MLSourceNames.extractBaseName(getMLRequest().getSourceConfig());
        modelNameField.setText(config.getModelName() != null ? config.getModelName() : "");
        if (config.getModelName() == null) {
            updateGeneratedModelName(generatedSourceName);
        }

        MLTrainerType trainerType = config.getTrainerType();
        if (trainerType == null) trainerType = MLTrainerType.SVM_CLASSIFICATION;

        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        MLMiningFunction miningFunction = toolboxForm != null ? toolboxForm.getMLRequest().getMiningFunction() : MLMiningFunction.CLASSIFICATION;
        refreshTrainers(miningFunction);
        if (trainerType.supportsDatabaseVersion(getConnection().getDatabaseVersion())) {
            algorithmComboBox.setSelectedValue(trainerType);
        }

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
        return txt("cfg.machineLearning.title.TrainingConfiguration");
    }

    @Override
    public String getFormTitleDetail() {
        MLTrainerType trainerType = algorithmComboBox.getSelectedValue();
        if (trainerType == null) return null;

        int trainPercent = splitSlider.getValue();
        return txt(
                "cfg.machineLearning.text.TrainingConfigurationDetail",
                trainerType.getName(),
                trainPercent,
                100 - trainPercent);
    }
}
