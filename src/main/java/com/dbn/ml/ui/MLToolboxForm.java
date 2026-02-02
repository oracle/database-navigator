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

package com.dbn.ml.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.ml.DatabaseMLManager;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.ui.backend.MLBackendForm;
import com.dbn.ml.ui.feature.MLFeatureForm;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.ml.ui.trainer.MLTrainerForm;
import com.intellij.openapi.Disposable;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class MLToolboxForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel backendPanel;
    private JPanel sourcePanel;
    private JPanel featurePanel;
    private JPanel trainerPanel;

    private MLBackendForm backendForm;
    private MLSourceForm sourceForm;
    private MLFeatureForm featureForm;
    private MLTrainerForm trainerForm;

    private final MLRequest request;

    public MLToolboxForm(Disposable parent, ConnectionHandler connection, MLRequest request) {
        super(parent, connection);
        this.request = request;

        initHeaderPanel();
        initHintPanel();
        initForms();
        resetFormChanges();
        updateFieldAlignment();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(backendForm, sourceForm, featureForm, trainerForm);
    }

    private void initForms() {
        ConnectionHandler connection = getConnection();

        // Backend configuration panel
        com.dbn.ml.model.MLBackendConfig backendConfig = request.getBackendConfig();
        backendForm = new MLBackendForm(this, connection);
        DBNCollapsiblePanel backendCollapsiblePanel = new DBNCollapsiblePanel(this, backendForm, true);
        backendCollapsiblePanel.addToggleListener(expanded -> backendConfig.setExpanded(expanded));
        backendPanel.add(backendCollapsiblePanel.getComponent());

        // Source configuration panel
        MLSourceConfig sourceConfig = request.getSourceConfig();
        sourceForm = new MLSourceForm(this, connection);
        DBNCollapsiblePanel sourceCollapsiblePanel = new DBNCollapsiblePanel(this, sourceForm, true);
        sourceCollapsiblePanel.addToggleListener(expanded -> sourceConfig.setExpanded(expanded));
        sourcePanel.add(sourceCollapsiblePanel.getComponent());

        // Feature configuration panel
        MLFeatureConfig featureConfig = request.getFeatureConfig();
        featureForm = new MLFeatureForm(this, connection);
        DBNCollapsiblePanel featureCollapsiblePanel = new DBNCollapsiblePanel(this, featureForm, true);
        featureCollapsiblePanel.addToggleListener(expanded -> featureConfig.setExpanded(expanded));
        featurePanel.add(featureCollapsiblePanel.getComponent());

        // Trainer configuration panel
        MLTrainerConfig trainerConfig = request.getTrainerConfig();
        trainerForm = new MLTrainerForm(this, connection);
        DBNCollapsiblePanel trainerCollapsiblePanel = new DBNCollapsiblePanel(this, trainerForm, true);
        trainerCollapsiblePanel.addToggleListener(expanded -> trainerConfig.setExpanded(expanded));
        trainerPanel.add(trainerCollapsiblePanel.getComponent());
    }

    public MLRequest getMLRequest() {
        return request;
    }

    public MLSourceForm getSourceForm() {
        return sourceForm;
    }

    public void resetFormChanges() {
        backendForm.resetFormChanges();
        sourceForm.resetFormChanges();
        featureForm.resetFormChanges();
        trainerForm.resetFormChanges();
    }

    public void applyFormChanges() {
        backendForm.applyFormChanges();
        sourceForm.applyFormChanges();
        featureForm.applyFormChanges();
        trainerForm.applyFormChanges();
    }

    public void saveRequestTemplate(boolean reset) {
        MLRequest requestTemplate = request.clone();
        if (reset) {
            SchemaId userSchema = getConnection().getUserSchemaId();
            requestTemplate.reset(userSchema);
        }

        ConnectionId connectionId = getConnectionId();
        DatabaseMLManager mlManager = DatabaseMLManager.getInstance(getProject());
        mlManager.setRequestTemplate(connectionId, requestTemplate);
    }

    protected void reset() {
        SchemaId userSchema = getConnection().getUserSchemaId();
        request.reset(userSchema);
        saveRequestTemplate(false);
        resetFormChanges();
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain(
                "Machine Learning Toolbox\n\n" +
                "Use this interface to build and train machine learning models. " +
                "Choose between Tribuo (client-side) or Oracle DBMS_DATA_MINING (in-database) training. " +
                "Select your data source, choose features and a label column, " +
                "configure the training algorithm, and train your model.\n\n" +
                "The trained model can be evaluated and used for predictions. " +
                "Tribuo models can also be exported to ONNX format.");
        DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
    
    // Called when source table changes - refresh available columns
    public void onSourceChanged() {
        if (featureForm != null) {
            featureForm.refreshColumns();
        }
    }

    // Called when backend type changes - refresh available trainers
    public void onBackendChanged() {
        // Apply backend changes first so refreshTrainers reads the new value
        if (backendForm != null) {
            backendForm.applyFormChanges();
        }
        if (trainerForm != null) {
            trainerForm.refreshTrainers();
        }
    }
}
