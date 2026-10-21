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

package com.dbn.ml.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.connection.SchemaId;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.ml.model.source.MLTableSourceConfig;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.ml.model.source.MLSourceType.DATABASE_TABLE;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.MINING_MODEL;
import static com.dbn.object.type.DBObjectType.TABLE;

public class MLResultSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel messagePanel;
    private DBNHyperlinkLabel modelHyperlinkLabel;
    private DBNHyperlinkLabel sourceTableHyperlinkLabel;
    private JLabel taskLabel;
    private JLabel algorithmLabel;
    private JLabel sourceTypeLabel;
    private JLabel sourceLabel;
    private JLabel targetColumnLabel;
    private JLabel featuresLabel;
    private JLabel trainingSamplesLabel;
    private JLabel testSamplesLabel;
    private JLabel trainingTimeCaption;
    private JLabel trainingTimeLabel;
    private JLabel classesCaption;
    private JLabel classesLabel;
    private JLabel outputDimensionsCaption;
    private JLabel outputDimensionsLabel;

    private final MLExecutionResult executionResult;
    private final DBNMessageForm messageForm;

    public MLResultSummaryForm(@Nullable Disposable parent, @NotNull MLExecutionResult executionResult) {
        super(parent);
        this.executionResult = executionResult;
        messageForm = new DBNMessageForm(this, executionResult.getSummaryMessage());
        messagePanel.add(messageForm.getComponent());
        modelHyperlinkLabel.setIcon(Icons.DBO_MINING_MODEL);
        modelHyperlinkLabel.addHyperlinkListener(e -> navigateToModel());
        sourceTableHyperlinkLabel.setIcon(Icons.DBO_TABLE);
        sourceTableHyperlinkLabel.addHyperlinkListener(e -> navigateToSourceTable());
        refresh();
    }

    public void refresh() {
        MLResult result = executionResult.getMlResult();
        String modelName = result.getModelName();
        if (modelName == null) modelName = result.getRequest().getTrainerConfig().getTargetModelName();
        SchemaId schemaId = result.getModelHandle() == null ?
                result.getRequest().getTrainerConfig().getTargetSchemaId() :
                result.getModelHandle().getTargetSchemaId();
        if (schemaId == null) schemaId = result.getConnection().getUserSchemaId();
        String qualifiedModelName = schemaId == null ? modelName : schemaId.getName() + "." + modelName;
        modelHyperlinkLabel.setHyperlinkText(qualifiedModelName);
        modelHyperlinkLabel.setToolTipText(qualifiedModelName);
        modelHyperlinkLabel.setEnabled(result.getModelHandle() != null);
        taskLabel.setText(result.isClassification() ?
                txt("app.machineLearning.const.MLTaskType_CLASSIFICATION") :
                txt("app.machineLearning.const.MLTaskType_REGRESSION"));
        algorithmLabel.setText(result.getAlgorithmName());
        sourceTypeLabel.setText(result.getRequest().getSourceConfig().getSourceType().getName());
        String sourceName = MLSourceNames.getDisplayName(result.getRequest().getSourceConfig());
        sourceLabel.setText(sourceName);
        sourceLabel.setToolTipText(sourceName);
        boolean tableSource = result.getRequest().getSourceConfig().getSourceType() == DATABASE_TABLE;
        sourceLabel.setVisible(!tableSource);
        sourceTableHyperlinkLabel.setVisible(tableSource);
        sourceTableHyperlinkLabel.setHyperlinkText(sourceName);
        String targetColumn = result.getRequest().getFeatureConfig().getLabelColumn();
        targetColumnLabel.setText(targetColumn);
        targetColumnLabel.setToolTipText(targetColumn);
        featuresLabel.setText(String.valueOf(result.getFeatureCount()));
        trainingSamplesLabel.setText(String.valueOf(result.getTrainingDataSize()));
        testSamplesLabel.setText(String.valueOf(result.getTestingDataSize()));
        trainingTimeLabel.setText(presentableDuration(result.getTrainingTimeMs(), false));
        classesLabel.setText(String.valueOf(result.getClassCount()));
        outputDimensionsLabel.setText(String.valueOf(result.getOutputDimensions()));

        boolean available = !executionResult.isLoading() && executionResult.getLoadError() == null;
        trainingTimeCaption.setVisible(available);
        trainingTimeLabel.setVisible(available);
        classesCaption.setVisible(available && result.isClassification());
        classesLabel.setVisible(available && result.isClassification());
        outputDimensionsCaption.setVisible(available && result.isRegression());
        outputDimensionsLabel.setVisible(available && result.isRegression());
        messageForm.setMessage(executionResult.getSummaryMessage());
    }

    private void navigateToModel() {
        MLResult result = executionResult.getMlResult();
        if (result.getModelHandle() == null) return;
        SchemaId schemaId = result.getModelHandle().getTargetSchemaId();
        if (schemaId == null) schemaId = result.getConnection().getUserSchemaId();
        if (schemaId == null) return;
        navigateToObject(schemaId.getName(), result.getModelName(), MINING_MODEL);
    }

    private void navigateToSourceTable() {
        MLTableSourceConfig source = executionResult.getMlResult().getRequest().getSourceConfig().getTableSourceConfig();
        navigateToObject(source.getSchemaName(), source.getTableName(), TABLE);
    }

    private void navigateToObject(String schemaName, String objectName, DBObjectType objectType) {
        if (schemaName == null || objectName == null) return;
        DBSchema schema = executionResult.getConnection().getObjectBundle().getSchema(schemaName);
        if (schema == null) return;
        DBObject object = schema.getChildObject(objectType, objectName);
        if (object != null) object.navigate(true);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
