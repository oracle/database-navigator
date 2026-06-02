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
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.cache.DBObjectNameCache;
import com.dbn.object.cache.DBObjectNameCacheListener;
import com.dbn.object.common.ui.DBObjectSelector;
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
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.object.cache.DBObjectFilterType.EMBEDDING_DESTINATION_TABLES;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

public class EmbeddingDestinationConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;

    private final Latent<DBObjectSpec> tableSpec = Latent.basic(() -> createTableFactoryInput());

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
                .triggerLoad();
    }


    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, e -> populateTables());
        onSelectionChange(tableComboBox, e -> populateColumns());
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(tableComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
        alignerData.registerFieldGroup(tableLabel, tableComboBox);
    }

    protected List<DBTable> loadTables() {
        List<DBTable> tables = super.loadTables();
        DBObjectNameCache<DBTable> names = getDestinationTablesCache();
        return names.filter(tables);
    }

    private DBObjectNameCache<DBTable> getDestinationTablesCache() {
        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
        return vectorManager.getObjectNamesCache(getConnectionId(), EMBEDDING_DESTINATION_TABLES);
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox, txt("msg.shared.error.SelectSchema"));
        addSelectionValidation(tableComboBox, txt("msg.shared.error.SelectTable"));
    }

    private void populateColumns() {
        updateFieldAvailability();
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
