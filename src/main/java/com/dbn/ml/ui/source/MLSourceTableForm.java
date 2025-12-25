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
import com.dbn.ml.ui.MLToolboxForm;
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

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

/**
 * Form for database table source selection.
 * Follows VectorToolbox pattern (EmbeddingSourceTableForm).
 */
public class MLSourceTableForm extends MLToolboxFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;

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
        onSelectionChange(schemaComboBox, s -> populateTables());
        onSelectionChange(tableComboBox, t -> onTableChanged());
    }

    private void initComboBoxes() {
        MLTableSourceConfig config = getConfig();

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
                .withValueLoader(() -> loadTablesForSchema())
                .withValuePreselector(() -> config.getTableName())
                .triggerLoad();

        updateFieldAvailability();
    }

    private List<DBTable> loadTablesForSchema() {
        DBSchema schema = getSelectedSchema();
        return schema == null ? List.of() : loadTables(schema);
    }

    private void populateTables() {
        updateFieldAvailability();
        tableComboBox.reloadValues();
        notifySourceChanged();
    }

    private void onTableChanged() {
        notifySourceChanged();
    }

    private void notifySourceChanged() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm != null) {
            toolboxForm.onSourceChanged();
        }
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return schemaComboBox != null ? schemaComboBox.getSelectedValue() : null;
    }

    @Nullable
    public DBTable getSelectedTable() {
        return tableComboBox != null ? tableComboBox.getSelectedValue() : null;
    }

    private MLTableSourceConfig getConfig() {
        MLToolboxForm toolboxForm = getParentFrom(MLToolboxForm.class);
        if (toolboxForm == null) return new MLTableSourceConfig();
        return toolboxForm.getMLRequest().getSourceConfig().getTableSourceConfig();
    }

    @Override
    public void resetFormChanges() {
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
