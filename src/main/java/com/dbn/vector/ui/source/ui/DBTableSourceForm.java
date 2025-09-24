package com.dbn.vector.ui.source.ui;

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.resetComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.data.type.GenericDataType.LITERAL;

public class DBTableSourceForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private DBNComboBox<DBTable> sourceTableComboBox;

  private DBNComboBox<DBColumn> sourceDataColumnComboBox;
  private DBNComboBox<DBColumn> sourceColumnIdComboBox;
  private JCheckBox autoSyncCheckBox;
  private JPanel initializingIconPanel;

  private ConnectionHandler connectionHandler;

  public DBTableSourceForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    System.out.println("hhhhfj");
    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    initComboboxListeners();
    initValidation();
    whenShown(() -> populateSchemas());
  }

  private void initComboboxListeners() {
    schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceTableComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceDataColumnComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceColumnIdComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    schemaComboBox.addListener((ov,nv)-> populateTables(nv));
    sourceTableComboBox.addListener((ov,nv)-> populateColumns(nv));
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(sourceTableComboBox,"Please select a  table");
    addSelectionValidation(sourceDataColumnComboBox,"Please select a  data column");
    addSelectionValidation(sourceColumnIdComboBox,"Please select a  column id");

  }

  private void populateColumns(DBTable table) {
    resetComboBox(sourceColumnIdComboBox);
    resetComboBox(sourceDataColumnComboBox);
    if (table == null) return;

    Background.run(()->{
      try{
        startActivityNotifier();
        List<DBColumn> columns = table.getColumns();
        List<DBColumn> primaryKeyColumns = table.getPrimaryKeyColumns();
        DBColumn idColumn = primaryKeyColumns.isEmpty() ? null : primaryKeyColumns.get(0);
        // find first literal column
        DBColumn dataColumn = Lists.first(columns, c ->
                c.getDataType().isNative() &&
                c.getDataType().getGenericDataType().is(LITERAL));

        Dispatch.run(mainPanel, () -> {
          initComboBox(sourceColumnIdComboBox, columns);
          initComboBox(sourceDataColumnComboBox, columns);
          setSelection(sourceColumnIdComboBox, idColumn);
          setSelection(sourceDataColumnComboBox, dataColumn);
        });
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void populateSchemas() {
    resetComboBox(schemaComboBox);
    Background.run(() -> {
      try {
        startActivityNotifier();
        DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
        List<DBSchema> schemas = objectBundle.getSchemas();
        DBSchema schema = objectBundle.getUserSchema();

        Dispatch.run(mainPanel, () -> {
          initComboBox(schemaComboBox, schemas);
          setSelection(schemaComboBox, schema);
        });
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void populateTables(DBSchema schema) {
    resetComboBox(sourceTableComboBox);
    resetComboBox(sourceDataColumnComboBox);
    resetComboBox(sourceColumnIdComboBox);
    if (schema == null) return;

    Background.run(() -> {
      startActivityNotifier();
      try {
        List<DBTable> tables = schema.getTables();
        Dispatch.run(mainPanel, () -> initComboBox(sourceTableComboBox, tables));
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void startActivityNotifier() {
    initializingIconPanel.setVisible(true);
  }

  /**
   * Stops the spining wheel
   */
  private void stopActivityNotifier() {
    initializingIconPanel.setVisible(false);
  }


  public DBTableSourceConfig getDBTableSourceConfig() {
    DBTableSourceConfig dbTableSourceConfig = new DBTableSourceConfig();
    dbTableSourceConfig.setSourceSchema(schemaComboBox.getSelectedValue());
    dbTableSourceConfig.setSourceTable(sourceTableComboBox.getSelectedValue());
    dbTableSourceConfig.setDataColumn(sourceDataColumnComboBox.getSelectedValue());
    dbTableSourceConfig.setIdColumn(sourceColumnIdComboBox.getSelectedValue());
    dbTableSourceConfig.setAutoSync(autoSyncCheckBox.isSelected());

    return dbTableSourceConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
