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

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.object.DBConsole;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingModelConfig;
import com.dbn.vector.model.request.EmbeddingModelDatabaseSpec;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBSearchConsoleVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.object.type.DBObjectType.AI_MODEL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;
import static com.dbn.vector.model.request.EmbeddingModelLocation.IN_DATABASE_MODEL;
import static com.dbn.vector.model.request.EmbeddingSourceType.FILE_SYSTEM;
import static com.dbn.vfs.DBConsoleType.SEARCH;

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
    private JButton similaritySearchButton;
    private JButton databaseAssistantButton;

    private final VectorEmbeddingResult result;

    public EmbeddingResultSummaryForm(@Nullable Disposable parent, VectorEmbeddingResult result) {
        super(parent);
        this.result = result;

        initMessagePanel();
        initSummaryLabels();
        initButtons();
    }

    private void initButtons() {
        onButtonClick(similaritySearchButton, e -> openSimilaritySearchConsole());
        onButtonClick(databaseAssistantButton, e -> openDatabaseAssistant());
    }

    private void openSimilaritySearchConsole() {
        ConnectionHandler connection = result.getConnection();
        Project project = ensureProject();

        DBConsole console = getMatchingConsole();

        if (console != null) {
            DBConsoleVirtualFile consoleFile = console.getVirtualFile();
            Editors.openFileEditor(project, consoleFile, true);
        } else {
            DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(project);
            String consoleName = consoleManager.getNextConsoleName(connection);
            consoleManager.createConsole(connection, consoleName, "", SEARCH, c -> initConsole(c));
        }
    }

    private void initConsole(DBConsole c) {
        DBSearchConsoleVirtualFile consoleFile = (DBSearchConsoleVirtualFile) c.getVirtualFile();
        EmbeddingDestinationConfig destinationConfig = result.getRequest().getDestinationConfig();
        consoleFile.setSearchSchema(destinationConfig.getSchemaName());
        consoleFile.setSearchTable(destinationConfig.getTableName());
    }

    @Nullable
    private DBConsole getMatchingConsole() {
        ConnectionHandler connection = result.getConnection();
        List<DBConsole> consoles = connection.getConsoleBundle().getConsoles();

        for (DBConsole console : consoles) {
            if (matchesConsole(console)) return console;
        }
        return null;
    }

    private boolean matchesConsole(DBConsole console) {
        if (console.getConsoleType() != SEARCH) return false;
        DBSearchConsoleVirtualFile file = (DBSearchConsoleVirtualFile) console.getVirtualFile();
        EmbeddingDestinationConfig destinationConfig = result.getRequest().getDestinationConfig();

        if (!Objects.equals(file.getSearchSchema(), destinationConfig.getSchemaName())) return false;
        if (!Objects.equals(file.getSearchTable(), destinationConfig.getTableName())) return false;

        return true;
    }

    private void openDatabaseAssistant() {
        Project project = ensureProject();
        ConnectionId connectionId = result.getConnectionId();

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        EmbeddingDestinationConfig destinationConfig = result.getRequest().getDestinationConfig();
        String schemaName = destinationConfig.getSchemaName();
        String tableName = destinationConfig.getTableName();
        DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, SCHEMA, schemaName);
        DBObjectRef<DBTable> table = new DBObjectRef<>(schema, TABLE, tableName);

        // remember as recent selection
        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
        Set<DBObjectRef<DBTable>> embeddingTables = vectorManager.getRecentEmbeddingTables(connectionId);
        embeddingTables.add(table);

        assistantManager.startAssistantChat(
                result.getId(),
                connectionId,
                AssistantType.PUBLIC,
                AssistantMode.RAG,
                table);
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
                TABLE));


        EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
        embeddingTableHyperlinkLabel.setIcon(Icons.DBO_TABLE);
        embeddingTableHyperlinkLabel.setHyperlinkText(destinationConfig.getQualifiedTableName());
        embeddingTableHyperlinkLabel.addHyperlinkListener(e -> navigateToObject(
                destinationConfig.getSchemaName(),
                destinationConfig.getTableName(),
                TABLE));

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
                AI_MODEL);
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
