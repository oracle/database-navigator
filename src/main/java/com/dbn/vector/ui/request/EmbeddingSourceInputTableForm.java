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

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

public class EmbeddingSourceInputTableForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JLabel schemaLabel;
    private JLabel tableLabel;
    private JLabel keyColumnLabel;
    private JLabel dataColumnLabel;

    private DBObjectSelector<DBSchema> schemaComboBox;
    private DBObjectSelector<DBTable> tableComboBox;
    private DBObjectSelector<DBColumn> keyColumnComboBox;
    private DBObjectSelector<DBColumn> dataColumnComboBox;
    private JPanel headerPanel;
    private JPanel hintPanel;

    private final ConnectionRef connection;
    private final EmbeddingSourceTable config;

    public EmbeddingSourceInputTableForm(@NotNull Disposable parent, ConnectionHandler connection, EmbeddingSourceTable config) {
        super(parent);
        this.connection = connection.ref();
        this.config = config;

        initHeaderPanel();
        initHintPanel();

        resetFormChanges();
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
        headerPanel.add(headerForm.getComponent());
    }

    private void initHintPanel() {
        TextContent hint = TextContent.plain("Please specify the table, the primary‑key column, and the column containing the text to be embedded.\n\n" +
                "NOTE: Table and column information will be recorded as metadata in the embedding results, so each vector can be traced back to its original record.");
        DBNHintForm hintForm = new DBNHintForm(this, hint, null, true);
        hintPanel.add(hintForm.getComponent());

    }

    protected VectorEmbeddingRequest getEmbeddingRequest() {
        return null;
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public ConnectionId getConnectionId() {
        return connection.getId();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(tableComboBox));
        fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTable()), array(
                keyColumnComboBox,
                dataColumnComboBox));
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
        alignerData.registerFieldGroup(tableLabel, tableComboBox);
        alignerData.registerFieldGroup(keyColumnLabel, keyColumnComboBox);
        alignerData.registerFieldGroup(dataColumnLabel, dataColumnComboBox);
    }

    private void initComboBoxes() {
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
                .withValuePreselector(() -> config.getDataColumnName())
                .triggerLoad();

        updateFieldAvailability();
    }

    private List<DBColumn> loadKeyColumns() {
        DBTable table = ComboBoxes.getSelection(tableComboBox);
        return table == null ?
                Collections.emptyList() :
                table.getPrimaryKeyColumns();
    }

    private List<DBColumn> loadDataColumns() {
        DBTable table = ComboBoxes.getSelection(tableComboBox);
        List<DBColumn> columns = table == null ?
                Collections.emptyList() :
                table.getColumns();

        return Lists.filter(columns, c -> c.getDataType().isLiteral() && !c.isPrimaryKey() && !c.isHidden());
    }

    protected void initEventListeners() {
        onSelectionChange(schemaComboBox, v -> populateTables());
        onSelectionChange(tableComboBox, v -> populateColumns());
    }

    @Override
    protected void initValidation() {
        addSelectionValidation(schemaComboBox, txt("msg.shared.error.SelectSchema"));
        addSelectionValidation(tableComboBox, txt("msg.shared.error.SelectTable"));
        addSelectionValidation(keyColumnComboBox, txt("msg.vector.error.SelectPrimaryKeyColumn"));
        addSelectionValidation(dataColumnComboBox, txt("msg.vector.error.SelectDataColumn"));
    }

    private void populateColumns() {
        updateFieldAvailability();
        keyColumnComboBox.reloadValues();
        dataColumnComboBox.reloadValues();
    }

    private void populateTables() {
        updateFieldAvailability();
        tableComboBox.reloadValues();
        keyColumnComboBox.reloadValues();
        dataColumnComboBox.reloadValues();
    }

    @Override
    public void resetFormChanges() {
        initComboBoxes();
    }

    @Override
    public void applyFormChanges() {
        appyFormChanges(config);
    }

    private void appyFormChanges(EmbeddingSourceTable config) {
        config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
        config.setTableName(getSelectedObjectName(tableComboBox, config.getTableName()));
        config.setKeyColumnName(getSelectedObjectName(keyColumnComboBox, config.getKeyColumnName()));
        config.setDataColumnName(getSelectedObjectName(dataColumnComboBox, config.getDataColumnName()));
    }

    @Nullable
    public DBSchema getSelectedSchema() {
        return ComboBoxes.getSelection(schemaComboBox);
    }

    @Nullable
    public DBTable getSelectedTable() {
        return tableComboBox.getSelectedValue();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
