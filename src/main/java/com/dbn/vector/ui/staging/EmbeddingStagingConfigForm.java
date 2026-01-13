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

package com.dbn.vector.ui.staging;

import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecReader;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

public class EmbeddingStagingConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private DBNInfoLabel tableInfoLabel;
    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;

    public EmbeddingStagingConfigForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        initInfoLabel();
    }

    private void initInfoLabel() {
        String info = TextResources.get(this, "staging_table_info.html.ft");
        TextContent infoContent = TextContent.html(info);
        tableInfoLabel.setContent(infoContent);
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

    private void initComboBoxes() {
        EmbeddingStagingConfig config = getConfig();

        schemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> config.getSchemaName())
                .triggerLoad();

        tableComboBox.initialize(this, TABLE)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getSelectedSchema())
                .withValueLoader(() -> loadTables())
                .withValuePreselector(() -> config.getTableName())
                .withObjectFactory("New Table...")
                .withValueFactoryInput(() -> createTableFactoryInput())
                .triggerLoad();

        updateFieldAvailability();
    }

    protected List<DBTable> loadTables() {
        List<DBTable> tables = super.loadTables();
        return Lists.filter(tables, t -> isStagingTable(t));
    }

    private boolean isStagingTable(DBTable table) {
        // TODO create generic DBObjectFactoryInput.matchesObject();

        List<DBColumn> columns = table.getColumns();
        if (columns.size() < 5) return false; // no exact match expected (consider system columns)

        Set<String> columnNames = columns.stream().map(c -> c.getName()).collect(Collectors.toSet());
        Set<String> expectedColumnNames = Set.of("ID", "FILE_SIZE", "FILE_HASH", "FILE_CONTENT", "METADATA");
        if (!columnNames.containsAll(expectedColumnNames)) return false;

        return true;
    }

    private DBObjectSpec createTableFactoryInput() {
        DBObjectSpec tableSpec = DBObjectSpecReader.read(getClass(), "staging-table-definition.xml");
        tableSpec.setConnectionId(getConnectionId());
        tableSpec.setSchemaId(getSelectedSchemaId());
        return tableSpec;
    }

    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, v -> populateTables());
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox,"Please select a schema");
        addSelectionValidation(tableComboBox,"Please select a table");
    }

    private void populateTables() {
        updateFieldAvailability();
        tableComboBox.reloadValues();
    }

    @Override
    public void resetFormChanges() {
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        EmbeddingStagingConfig config = getConfig();
        config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
        config.setTableName(getSelectedObjectName(tableComboBox, config.getTableName()));
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return ComboBoxes.getSelection(schemaComboBox);
    }

    @Nullable
    public DBTable getSelectedTable() {
        return tableComboBox.getSelectedValue();
    }

    public EmbeddingStagingConfig getConfig() {
        return getEmbeddingRequest().getStagingConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Staging Location";
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
