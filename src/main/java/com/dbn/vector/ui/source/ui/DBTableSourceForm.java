package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;

public class DBTableSourceForm extends DBNFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private DBNComboBox<DBTable> tableComboBox;

  private DBNComboBox<DBColumn> keyColumnComboBox;
  private DBNComboBox<DBColumn> dataColumnComboBox;
  private JCheckBox autoSyncCheckBox;

  private final ConnectionRef connection;

  public DBTableSourceForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent);
    this.connection = connection.ref();

    initComboboxListeners();
    whenShown(() -> initComboBoxes());
  }

  private void initComboBoxes() {
    schemaComboBox.setValueLoader(() -> loadSchemas());
    tableComboBox.setValueLoader(() -> loadTables());
    keyColumnComboBox.setValueLoader(() -> loadKeyColumns());
    dataColumnComboBox.setValueLoader(() -> loadDataColumns());

    schemaComboBox.loadValues();
    tableComboBox.loadValues();
    keyColumnComboBox.loadValues();
    dataColumnComboBox.loadValues();
  }

  private void refreshComboBoxes() {
      DBSchema schema = getSelectedSchema();
      DBTable table = getSelectedTable();
      tableComboBox.setEnabled(schema != null);
      keyColumnComboBox.setEnabled(table != null);
      dataColumnComboBox.setEnabled(table != null);
  }

  private List<DBSchema> loadSchemas() {
    DBObjectBundle objectBundle = getConnection().getObjectBundle();
    return objectBundle.getSchemas();
  }

  private List<DBTable> loadTables() {
    DBSchema schema = getSelectedSchema();
    return schema == null ?
            Collections.emptyList() :
            schema.getTables();
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

  private void initComboboxListeners() {
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    tableComboBox.set(HIDE_DESCRIPTION, true);
    dataColumnComboBox.set(HIDE_DESCRIPTION, true);
    keyColumnComboBox.set(HIDE_DESCRIPTION, true);

    schemaComboBox.addListener((ov,nv)-> populateTables());
    tableComboBox.addListener((ov, nv)-> populateColumns());
  }

  @Override
  protected void initValidation() {

    // TODO fix validation for hidden fields
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(tableComboBox,"Please select a table");
    addSelectionValidation(keyColumnComboBox,"Please select the primary key column");
    addSelectionValidation(dataColumnComboBox,"Please select a data column");

  }

  private void populateColumns() {
    refreshComboBoxes();
    keyColumnComboBox.reloadValues();
    dataColumnComboBox.reloadValues();
  }

  private void populateTables() {
    refreshComboBoxes();
    tableComboBox.reloadValues();
    keyColumnComboBox.reloadValues();
    dataColumnComboBox.reloadValues();
  }

  @Nullable
  private DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  @Nullable
  private DBTable getSelectedTable() {
    return tableComboBox.getSelectedValue();
  }

  public DBTableSourceConfig getConfiguration() {
    DBTableSourceConfig config = new DBTableSourceConfig();
    config.setSourceSchema(getSelectedSchema());
    config.setSourceTable(getSelectedTable());
    config.setDataColumn(dataColumnComboBox.getSelectedValue());
    config.setIdColumn(keyColumnComboBox.getSelectedValue());
    config.setAutoSync(autoSyncCheckBox.isSelected());

    return config;
  }

  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
