package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
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
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class DBTableSourceForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private DBNComboBox<DBTable> tableComboBox;

  private DBNComboBox<DBColumn> keyColumnComboBox;
  private DBNComboBox<DBColumn> dataColumnComboBox;
  private JCheckBox autoSyncCheckBox;
  private JLabel schemaLabel;
  private JLabel tableLabel;
  private JLabel keyColumnLabel;
  private JLabel dataColumnLabel;

  public DBTableSourceForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);

    initComboBoxes();
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
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    tableComboBox.set(HIDE_DESCRIPTION, true);
    dataColumnComboBox.set(HIDE_DESCRIPTION, true);
    keyColumnComboBox.set(HIDE_DESCRIPTION, true);

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

    return Lists.filter(columns, c -> c.getDataType().isLiteral());
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

    schemaComboBox.init(() -> loadSchemas(), s -> matchesObjectName(s, config.getSchemaName()));
    tableComboBox.init(() -> loadTables(), t -> matchesObjectName(t, config.getTableName()));
    keyColumnComboBox.init(() -> loadKeyColumns(), c -> matchesObjectName(c, config.getKeyColumnName()));
    dataColumnComboBox.init(() -> loadDataColumns(), c -> matchesObjectName(c, config.getDataColumnName()));
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
