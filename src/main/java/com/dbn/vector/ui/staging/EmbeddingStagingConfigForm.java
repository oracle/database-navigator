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
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.staging.StagingConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class EmbeddingStagingConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private DBNComboBox<DBSchema> schemaComboBox;
    private DBNComboBox<DBTable> tableComboBox;
    private DBNInfoLabel tableInfoLabel;
    private JLabel schemaLabel;
    private JLabel tableLabel;

    public EmbeddingStagingConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);

        initComboBoxes();
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
        schemaComboBox.set(HIDE_DESCRIPTION, true);
        tableComboBox.set(HIDE_DESCRIPTION, true);

        updateFieldAvailability();
    }

    private List<DBColumn> loadKeyColumns() {
        DBTable table = ComboBoxes.getSelection(tableComboBox);
        return table == null ?
                Collections.emptyList() :
                table.getPrimaryKeyColumns();
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
        StagingConfig config = getConfig();

        schemaComboBox.init(() -> loadSchemas(), s -> matchesObjectName(s, config.getSchemaName()));
        tableComboBox.init(() -> loadTables(), t -> matchesObjectName(t, config.getTableName()));
    }

    @Override
    public void applyFormChanges() {
        StagingConfig config = getConfig();
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

    public StagingConfig getConfig() {
        return getEmbeddingRequest().getStagingConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Staging Configuration";
    }

    @Override
    public String getFormTitleDetail() {
        return "";
    }
}
