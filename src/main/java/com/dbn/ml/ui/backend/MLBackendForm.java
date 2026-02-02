/*
 * Copyright 2024-2025 Oracle and/or its affiliates
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

package com.dbn.ml.ui.backend;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.model.MLBackendConfig;
import com.dbn.ml.ui.MLToolboxForm;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.intellij.openapi.Disposable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.*;

/**
 * Form for ML backend selection (Tribuo vs DBMS_DATA_MINING).
 *
 * @author ayoub allali
 */
public class MLBackendForm extends MLToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private DBNComboBox<MLBackendType> backendTypeComboBox;
    private JLabel backendTypeLabel;
    private JCheckBox autoCleanupCheckBox;
    private JLabel autoCleanupLabel;

    public MLBackendForm(Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initComponents();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(backendTypeLabel, backendTypeComboBox);
        alignerData.registerFieldGroup(autoCleanupLabel, autoCleanupCheckBox);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(backendTypeComboBox, t -> onBackendTypeChanged());
    }

    private void initComponents() {
        // Backend type combo
        initComboBox(backendTypeComboBox, MLBackendType.values());
        setSelection(backendTypeComboBox, MLBackendType.TRIBUO);

        // Auto-cleanup checkbox (default enabled)
        autoCleanupCheckBox.setSelected(true);

        onBackendTypeChanged();
    }

    private void onBackendTypeChanged() {
        MLBackendType selectedBackend = getSelection(backendTypeComboBox);

        // Show/hide auto-cleanup option based on backend type
        boolean isDBMS = selectedBackend == MLBackendType.DBMS_DATA_MINING;
        autoCleanupLabel.setVisible(isDBMS);
        autoCleanupCheckBox.setVisible(isDBMS);

        // Notify parent form that backend changed
        notifyBackendChanged();
    }

    private void notifyBackendChanged() {
        // Notify MLToolboxForm to update trainer options
        MLToolboxForm toolboxForm = getToolboxForm();
        if (toolboxForm != null) {
            toolboxForm.onBackendChanged();
        }
    }

    @Override
    public void resetFormChanges() {
        MLBackendConfig config = getConfig();

        setSelection(backendTypeComboBox, config.getBackendType());
        autoCleanupCheckBox.setSelected(config.isAutoCleanupStagingTables());

        onBackendTypeChanged();
    }

    @Override
    public void applyFormChanges() {
        MLBackendConfig config = getConfig();

        config.setBackendType(getSelection(backendTypeComboBox));
        config.setAutoCleanupStagingTables(autoCleanupCheckBox.isSelected());
    }

    public MLBackendConfig getConfig() {
        return getMLRequest().getBackendConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Training Backend";
    }

    @Override
    public String getFormTitleDetail() {
        MLBackendType backendType = getSelection(backendTypeComboBox);
        return backendType == null ? "" : backendType.getName();
    }
}
