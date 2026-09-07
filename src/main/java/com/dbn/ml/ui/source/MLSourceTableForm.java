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

package com.dbn.ml.ui.source;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.model.source.MLTableSourceConfig;
import com.dbn.ml.ui.MLToolboxFormBase;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.ml.model.source.MLSourceType.DATABASE_TABLE;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

/**
 * Form for database table source selection.
 */
public class MLSourceTableForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;
    private String sourceSchemaName;
    private String sourceTableName;

    public MLSourceTableForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
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

    @Override
    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, this::onSchemaChanged);
        onSelectionChange(tableComboBox, t -> onTableChanged());
    }

    private void initComboBoxes() {
        MLTableSourceConfig config = getConfig();

        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getSchemaName())
                .withValueLoadConsumer(values -> onSchemasLoaded());

        tableComboBox.clearValues();
        tableComboBox
                .initialize(this, TABLE)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(List::of)
                .withValuePreselector(() -> config.getTableName())
                .withValueLoadConsumer(values -> onTablesLoaded());

        updateFieldAvailability();
        schemaComboBox.triggerLoad();
    }

    private void onSchemaChanged(DBSchema schema) {
        updateFieldAvailability();
        tableComboBox.withValueLoader(() -> schema == null ? List.of() : loadTables(schema));
        tableComboBox.reloadValues();
        validateFormFields();
        if (!LOADING.is(schemaComboBox)) {
            updateSourceSelection();
            notifySourceChanged();
        }
    }

    private void onTableChanged() {
        if (!LOADING.is(schemaComboBox) &&
                !LOADING.is(tableComboBox)) {
            updateSourceSelection();
            notifySourceChanged();
        }
    }

    private void onSchemasLoaded() {
        if (getSelectedSchema() == null && updateSourceSelection()) {
            notifySourceChanged();
        }
    }

    private void onTablesLoaded() {
        if (getSelectedSchema() == null) return;

        if (updateSourceSelection()) {
            notifySourceChanged();
        } else {
            notifySourceLoaded();
        }
    }

    private boolean updateSourceSelection() {
        DBSchema schema = getSelectedSchema();
        DBTable table = schema == null ? null : getSelectedTable();
        String schemaName = getObjectName(schema);
        String tableName = getObjectName(table);
        boolean changed = !Objects.equals(sourceSchemaName, schemaName) ||
                !Objects.equals(sourceTableName, tableName);
        sourceSchemaName = schemaName;
        sourceTableName = tableName;
        return changed;
    }

    private void notifySourceChanged() {
        ensureParentFrom(MLSourceForm.class).notifySourceChanged(DATABASE_TABLE);
    }

    private void notifySourceLoaded() {
        ensureParentFrom(MLSourceForm.class).notifySourceLoaded(DATABASE_TABLE);
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return schemaComboBox.getSelectedValue();
    }

    @Nullable
    public DBTable getSelectedTable() {
        return tableComboBox.getSelectedValue();
    }

    boolean isConfiguredSourceSelected() {
        MLTableSourceConfig config = getConfig();
        DBSchema schema = getSelectedSchema();
        DBTable table = getSelectedTable();
        return schema != null && table != null &&
                Objects.equals(schema.getName(), config.getSchemaName()) &&
                Objects.equals(table.getName(), config.getTableName());
    }

    private MLTableSourceConfig getConfig() {
        return getMLRequest().getSourceConfig().getTableSourceConfig();
    }

    @Override
    public void resetFormChanges() {
        MLTableSourceConfig config = getConfig();
        sourceSchemaName = config.getSchemaName();
        sourceTableName = config.getTableName();
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        MLTableSourceConfig config = getConfig();
        config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
        config.setTableName(getSelectedObjectName(tableComboBox, config.getTableName()));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
