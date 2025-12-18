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

package com.dbn.vector.ui.source;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.source.DBTableSourceConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class EmbeddingSourceTableForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JCheckBox autoSyncCheckBox;
  private JLabel schemaLabel;
  private JLabel tableLabel;
  private JLabel keyColumnLabel;
  private JLabel dataColumnLabel;

  private DBObjectSelector<DBSchema> schemaComboBox;
  private DBObjectSelector<DBTable> tableComboBox;
  private DBObjectSelector<DBColumn> keyColumnComboBox;
  private DBObjectSelector<DBColumn> dataColumnComboBox;

  public EmbeddingSourceTableForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
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
    ConnectionHandler connection = getConnection();
    DBTableSourceConfig config = getConfig();

    schemaComboBox.initialize(this, connection, DBObjectType.SCHEMA, () -> loadSchemas(), () -> config.getSchemaName());
    tableComboBox.initialize(this, connection, DBObjectType.TABLE, () -> loadTables(), () -> config.getTableName());
    keyColumnComboBox.initialize(this, connection, DBObjectType.COLUMN, () -> loadKeyColumns(), () -> config.getKeyColumnName());
    dataColumnComboBox.initialize(this, connection, DBObjectType.COLUMN, () -> loadDataColumns(), () -> config.getDataColumnName());

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

    return Lists.filter(columns, c -> c.getDataType().isLiteral() && !c.isPrimaryKey());
  }

  protected void initEventListeners() {
    onSelectionChange(schemaComboBox, v -> populateTables());
    onSelectionChange(tableComboBox, v -> populateColumns());
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(tableComboBox,"Please select a table");
    addSelectionValidation(keyColumnComboBox,"Please select the primary key column");
    addSelectionValidation(dataColumnComboBox,"Please select a data column");
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
    DBTableSourceConfig config = getConfig();
    autoSyncCheckBox.setSelected(config.isAutoSync());

    initComboBoxes();
  }

  @Override
  public void applyFormChanges() {
    DBTableSourceConfig config = getConfig();
    config.setAutoSync(autoSyncCheckBox.isSelected());
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

  public DBTableSourceConfig getConfig() {
    return getEmbeddingRequest().getSourceConfig().getTableSourceConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
