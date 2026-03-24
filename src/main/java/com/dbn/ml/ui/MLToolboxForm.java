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
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.ml.DatabaseMLManager;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.ui.feature.MLFeatureForm;
import com.dbn.ml.ui.source.MLSourceForm;
import com.dbn.ml.ui.trainer.MLTrainerForm;
import com.intellij.openapi.Disposable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class MLToolboxForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel sourcePanel;
    private JPanel featurePanel;
    private JPanel trainerPanel;

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
        alignerData.registerForms(sourceForm, featureForm, trainerForm);
    }

    private void initForms() {
        ConnectionHandler connection = getConnection();

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
        sourceForm.resetFormChanges();
        featureForm.resetFormChanges();
        trainerForm.resetFormChanges();
    }

    public void applyFormChanges() {
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
                "Use this interface to build and train machine learning models using Oracle DBMS_DATA_MINING. " +
                "Select your data source, choose features and a label column, " +
                "configure the training algorithm, and train your model.\n\n" +
                "The trained model will be stored in the database and can be evaluated and used for predictions.");
        DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Powered by",
                "Oracle DBMS_DATA_MINING",
                "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmapi/DBMS_DATA_MINING.html");
        hintPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
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
}
