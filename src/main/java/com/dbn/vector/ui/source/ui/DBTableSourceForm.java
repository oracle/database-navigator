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
  private DBNComboBox<DBTable> sourceTableComboBox;

  private DBNComboBox<DBColumn> sourceDataColumnComboBox;
  private DBNComboBox<DBColumn> sourceKeyColumnComboBox;
  private JCheckBox autoSyncCheckBox;

  private final ConnectionRef connection;

  public DBTableSourceForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent);
    this.connection = connection.ref();

    initComboboxListeners();
    initValidation();


    whenShown(() -> initComboBoxes());
  }

  private void initComboBoxes() {
    schemaComboBox.setValueLoader(() -> loadSchemas());
    sourceTableComboBox.setValueLoader(() -> loadTables());
    sourceKeyColumnComboBox.setValueLoader(() -> loadKeyColumns());
    sourceDataColumnComboBox.setValueLoader(() -> loadDataColumns());
  }

  private void refreshComboBoxes() {
      DBSchema schema = getSelectedSchema();
      DBTable table = getSelectedTable();
      sourceTableComboBox.setEnabled(schema != null);
      sourceKeyColumnComboBox.setEnabled(table != null);
      sourceDataColumnComboBox.setEnabled(table != null);
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
    DBTable table = ComboBoxes.getSelection(sourceTableComboBox);
    return table == null ?
            Collections.emptyList() :
            table.getPrimaryKeyColumns();
  }

  private List<DBColumn> loadDataColumns() {
    DBTable table = ComboBoxes.getSelection(sourceTableComboBox);
    List<DBColumn> columns = table == null ?
            Collections.emptyList() :
            table.getColumns();

    return Lists.filter(columns, c -> c.getDataType().isLiteral());
  }

  private void initComboboxListeners() {
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    sourceTableComboBox.set(HIDE_DESCRIPTION, true);
    sourceDataColumnComboBox.set(HIDE_DESCRIPTION, true);
    sourceKeyColumnComboBox.set(HIDE_DESCRIPTION, true);

    schemaComboBox.addListener((ov,nv)-> populateTables());
    sourceTableComboBox.addListener((ov,nv)-> populateColumns());
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(sourceTableComboBox,"Please select a table");
    addSelectionValidation(sourceKeyColumnComboBox,"Please select the primary key column");
    addSelectionValidation(sourceDataColumnComboBox,"Please select a data column");
  }

  private void populateColumns() {
    refreshComboBoxes();
    sourceKeyColumnComboBox.reloadValues();
    sourceDataColumnComboBox.reloadValues();
  }

  private void populateTables() {
    refreshComboBoxes();
    sourceTableComboBox.reloadValues();
    sourceKeyColumnComboBox.reloadValues();
    sourceDataColumnComboBox.reloadValues();
  }

  @Nullable
  private DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  @Nullable
  private DBTable getSelectedTable() {
    return sourceTableComboBox.getSelectedValue();
  }

  public DBTableSourceConfig getConfiguration() {
    DBTableSourceConfig config = new DBTableSourceConfig();
    config.setSourceSchema(getSelectedSchema());
    config.setSourceTable(getSelectedTable());
    config.setDataColumn(sourceDataColumnComboBox.getSelectedValue());
    config.setIdColumn(sourceKeyColumnComboBox.getSelectedValue());
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
