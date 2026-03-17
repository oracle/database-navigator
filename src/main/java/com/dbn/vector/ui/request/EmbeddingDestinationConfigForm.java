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

package com.dbn.vector.ui.request;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.latent.Latent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.cache.DBObjectNameCache;
import com.dbn.object.cache.DBObjectNameCacheListener;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.common.ui.DBObjectSelectorListener;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecReader;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.disableFormFields;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.enableFormFields;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.data.type.GenericDataType.JSON;
import static com.dbn.data.type.GenericDataType.VECTOR;
import static com.dbn.object.cache.DBObjectFilterType.EMBEDDING_DESTINATION_TABLES;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

public class EmbeddingDestinationConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private JLabel keyColumnLabel;
    private JLabel dataColumnLabel;
    private JLabel embeddingColumnLabel;
    private JLabel metadataColumnLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;
    private DBObjectSelector<DBColumn> keyColumnComboBox;
    private DBObjectSelector<DBColumn> dataColumnComboBox;
    private DBObjectSelector<DBColumn> embeddingColumnComboBox;
    private DBObjectSelector<DBColumn> metadataColumnComboBox;

    private final Latent<DBObjectSpec> tableSpec = Latent.basic(() -> createTableFactoryInput());
    private final DBObjectSelector<?>[] columnSelectors = new DBObjectSelector[] {
            keyColumnComboBox,
            dataColumnComboBox,
            embeddingColumnComboBox,
            metadataColumnComboBox};

    public EmbeddingDestinationConfigForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        ProjectEvents.subscribe(ensureProject(), this, DBObjectNameCacheListener.TOPIC, tableCacheListener());
    }

    private DBObjectNameCacheListener tableCacheListener() {
        return (connectionId, schemaId, objectType, filterType) -> {
            if (objectType != TABLE) return;
            if (connectionId != getConnectionId()) return;
            if (schemaId != getSelectedSchemaId()) return;
            if (filterType != EMBEDDING_DESTINATION_TABLES) return;

            tableComboBox.reloadValues();
        };
    }

    private void initComboBoxes() {
        EmbeddingDestinationConfig config = getConfig();
        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getSchemaName())
                .triggerLoad();

        tableComboBox
                .initialize(this, TABLE)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadTables())
                .withValuePreselector(() -> config.getTableName())
                .withObjectFactory("New Table...")
                .withValueFactoryInput(tableSpec)
                .withValueFactoryNameConsumer(() -> name -> getDestinationTablesCache().addObjectName(getSelectedSchemaId(), name))
                .withListener(tableLoadListener())
                .triggerLoad();

        keyColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadKeyColumns())
                .withValuePreselector(() -> config.getKeyColumnName())
                .triggerLoad();

        dataColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadDataColumns())
                .withValuePreselector(() -> config.getTextColumnName())
                .triggerLoad();

        embeddingColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadEmbeddingColumns())
                .withValuePreselector(() -> config.getEmbeddingColumnName())
                .triggerLoad();

        metadataColumnComboBox
                .initialize(this, COLUMN)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadMetadataColumns())
                .withValuePreselector(() -> config.getMetadataColumnName())
                .triggerLoad();
    }

    private @NotNull DBObjectSelectorListener tableLoadListener() {
        return new DBObjectSelectorListener() {
            @Override
            public void valueLoadStarted() {
                disableFormFields(columnSelectors, "TEMPORARY_LOAD");
            }

            @Override
            public void valueLoadEnded() {
                enableFormFields(columnSelectors, "TEMPORARY_LOAD");
            }
        };
    }


    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, e -> populateTables());
        onSelectionChange(tableComboBox, e -> populateColumns());
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(tableComboBox));
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTable()), array(
                keyColumnComboBox,
                dataColumnComboBox,
                embeddingColumnComboBox,
                metadataColumnComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
        alignerData.registerFieldGroup(tableLabel, tableComboBox);
        alignerData.registerFieldGroup(keyColumnLabel, keyColumnComboBox);
        alignerData.registerFieldGroup(dataColumnLabel, dataColumnComboBox);
        alignerData.registerFieldGroup(embeddingColumnLabel, embeddingColumnComboBox);
        alignerData.registerFieldGroup(metadataColumnLabel, metadataColumnComboBox);
    }

    protected List<DBTable> loadTables() {
        List<DBTable> tables = super.loadTables();
        DBObjectNameCache<DBTable> names = getDestinationTablesCache();
        return names.filter(tables);
    }

    private boolean isDestinationTable(DBTable table) {
        DBObjectNameCache<DBTable> tablesCache = getDestinationTablesCache();
        return tablesCache.accepts(table);
    }

    private DBObjectNameCache<DBTable> getDestinationTablesCache() {
        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
        return vectorManager.getObjectNamesCache(getConnectionId(), EMBEDDING_DESTINATION_TABLES);
    }

    private List<DBColumn> loadKeyColumns() {
        return filter(getAllColumns(), c -> c.isPrimaryKey());
    }

    private List<DBColumn> loadDataColumns() {
        return filter(getAllColumns(), c -> c.getDataType().isLiteral() && !c.isPrimaryKey() && !c.isHidden());
    }

    private List<DBColumn> loadEmbeddingColumns() {
        return filter(getAllColumns(), c -> c.getDataType().getGenericDataType() == VECTOR);
    }

    private List<DBColumn> loadMetadataColumns() {
        return filter(getAllColumns(), c -> c.getDataType().getGenericDataType() == JSON);
    }

    private @NotNull List<DBColumn> getAllColumns() {
        DBTable table = ComboBoxes.getSelection(tableComboBox);
        return table == null ?
                Collections.emptyList() :
                table.getColumns();
    }


    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox, "Please select a schema");
        addSelectionValidation(tableComboBox, "Please select a table");
        addSelectionValidation(dataColumnComboBox, "Please select the primary key column");
        addSelectionValidation(embeddingColumnComboBox, "Please select a data column");
        addSelectionValidation(metadataColumnComboBox, "Please select a metadata column");
    }

    private void populateColumns() {
        updateFieldAvailability();
        keyColumnComboBox.reloadValues();
        dataColumnComboBox.reloadValues();
        embeddingColumnComboBox.reloadValues();
        metadataColumnComboBox.reloadValues();
    }

    private void populateTables() {
        tableComboBox.reloadValues();
        populateColumns();
    }

    private DBObjectSpec createTableFactoryInput() {
        DBObjectSpec tableSpec = DBObjectSpecReader.read(getClass(), "embedding-destination-table-definition.xml");
        tableSpec.setConnectionId(getConnectionId());
        tableSpec.setSchemaId(getSelectedSchemaId());
        return tableSpec;
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return ComboBoxes.getSelection(schemaComboBox);
    }

    @Nullable
    public DBTable getSelectedTable() {
        return ComboBoxes.getSelection(tableComboBox);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void resetFormChanges() {
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        EmbeddingDestinationConfig config = getConfig();
        config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
        config.setTableName(getSelectedObjectName(tableComboBox, config.getTableName()));
        config.setTextColumnName(getSelectedObjectName(dataColumnComboBox, config.getTextColumnName()));
        config.setEmbeddingColumnName(getSelectedObjectName(embeddingColumnComboBox, config.getEmbeddingColumnName()));
        config.setMetadataColumnName(getSelectedObjectName(metadataColumnComboBox, config.getMetadataColumnName()));
    }

    public EmbeddingDestinationConfig getConfig() {
        return getEmbeddingRequest().getDestinationConfig();
    }

    @Override
    public String getFormTitle() {
        return "Embedding Destination";
    }

    @Override
    public String getFormTitleDetail() {
        DBSchema schema = getSelectedSchema();
        DBTable table = getSelectedTable();

        if (schema != null && table != null) {
            return schema.getName() + "." + table.getName();
        }

        return null;
    }
}