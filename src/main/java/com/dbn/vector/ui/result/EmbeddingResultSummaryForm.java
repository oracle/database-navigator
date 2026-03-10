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

package com.dbn.vector.ui.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingModelConfig;
import com.dbn.vector.model.request.EmbeddingModelDatabaseSpec;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.vector.model.request.EmbeddingModelLocation.IN_DATABASE_MODEL;
import static com.dbn.vector.model.request.EmbeddingSourceType.FILE_SYSTEM;

public class EmbeddingResultSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel messagePanel;
    private JLabel sourceTypeLabel;
    private JLabel sourceCountLabel;
    private JLabel embeddedRowsLabel;
    private JLabel successRateLabel;
    private JLabel taskDurationLabel;
    private DBNInfoLabel stagingTableInfoLabel;
    private DBNInfoLabel embeddingsTableInfoLabel;
    private DBNHyperlinkLabel stagingTableHyperlinkLabel;
    private DBNHyperlinkLabel embeddingTableHyperlinkLabel;
    private DBNHyperlinkLabel embeddingModelHyperlinkLabel;
    private JLabel stagingTableLabel;
    private JLabel embeddingModelLabel;

    private final VectorEmbeddingResult result;

    public EmbeddingResultSummaryForm(@Nullable Disposable parent, VectorEmbeddingResult result) {
        super(parent);
        this.result = result;

        initMessagePanel();
        initSummaryLabels();
    }

    private void initSummaryLabels() {
        VectorEmbeddingRequest request = result.getRequest();
        EmbeddingSourceConfig sourceConfig = request.getSourceConfig();
        sourceTypeLabel.setText(sourceConfig.getSourceType().getName());
        sourceCountLabel.setText(sourceConfig.getSourceCount() + "");

        embeddedRowsLabel.setText(result.getTotalInsertedRows() +"");
        successRateLabel.setText(result.getSuccessRate() + "%");
        taskDurationLabel.setText(presentableDuration(result.getDuration(), true));

        stagingTableInfoLabel.setContent(html(this, "info/embedding_staging_table_info.html.ft"));
        embeddingsTableInfoLabel.setContent(html(this, "info/embedding_destination_table_info.html.ft"));

        EmbeddingStagingConfig stagingConfig = request.getStagingConfig();
        stagingTableHyperlinkLabel.setIcon(Icons.DBO_TABLE);
        stagingTableHyperlinkLabel.setHyperlinkText(request.getStagingConfig().getQualifiedTableName());
        stagingTableHyperlinkLabel.addHyperlinkListener(e -> navigateToObject(
                stagingConfig.getSchemaName(),
                stagingConfig.getTableName(),
                DBObjectType.TABLE));


        EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
        embeddingTableHyperlinkLabel.setIcon(Icons.DBO_TABLE);
        embeddingTableHyperlinkLabel.setHyperlinkText(destinationConfig.getQualifiedTableName());
        embeddingTableHyperlinkLabel.addHyperlinkListener(e -> navigateToObject(
                destinationConfig.getSchemaName(),
                destinationConfig.getTableName(),
                DBObjectType.TABLE));

        embeddingModelHyperlinkLabel.setIcon(Icons.DBO_AI_MODEL);
        embeddingModelHyperlinkLabel.setHyperlinkText(request.getModelConfig().getDatabaseModelConfig().getQualifiedModelName());
        embeddingModelHyperlinkLabel.addHyperlinkListener(e -> navigateToModel());
    }

    private void navigateToModel() {
        EmbeddingModelConfig modelConfig = result.getRequest().getModelConfig();
        if (modelConfig.getModelLocation() != IN_DATABASE_MODEL) return;

        EmbeddingModelDatabaseSpec databaseModelConfig = modelConfig.getDatabaseModelConfig();
        navigateToObject(
                databaseModelConfig.getSchemaName(),
                databaseModelConfig.getModelName(),
                DBObjectType.AI_MODEL);
    }

    private void navigateToObject(String schemaName, String objectName, DBObjectType objectType) {
        if (schemaName == null) return;
        if (objectName == null) return;

        DBSchema schema = result.getConnection().getObjectBundle().getSchema(schemaName);
        if (schema == null) return;

        DBObject object = schema.getChildObject(objectType, objectName);
        if (object == null) return;

        object.navigate(true);
    }

    private void initMessagePanel() {
        TitledMessage message = result.getSummaryMessage();
        DBNMessageForm messageForm = new DBNMessageForm(this, message);
        messagePanel.add(messageForm.getComponent());
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(
                () -> result.getSourceType() == FILE_SYSTEM,
                array(stagingTableLabel,
                      stagingTableInfoLabel,
                      stagingTableHyperlinkLabel));

        fieldAdapter.initFieldsVisibility(
                () -> result.getRequest().getModelConfig().getModelLocation() == IN_DATABASE_MODEL,
                array(embeddingModelLabel,
                      embeddingModelHyperlinkLabel));

    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
