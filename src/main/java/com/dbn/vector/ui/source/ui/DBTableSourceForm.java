package com.dbn.vector.ui.source.ui;

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.misc.DBNComboBoxModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.common.CreateTableConfig;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.addItems;

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
    System.out.println("jfj");
    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    initComboboxListeners();
    loadSchemas();
  }

  private void initComboboxListeners() {
    schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceTableComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceDataColumnComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    sourceColumnIdComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    schemaComboBox.addListener((ov,nv)-> populateDatabseObjectTable(nv));
    sourceTableComboBox.addListener((ov,nv)-> populateColumns(nv));
  }

  private void populateColumns(DBTable table) {
    Background.run(()->{
      try{
        startActivityNotifier();
        DBColumn idColumn = table.getPrimaryKeyColumns().isEmpty() ? table.getColumns().get(0) : table.getPrimaryKeyColumns().get(0);
        DBColumn dataColumn = table.getColumns().get(1);

        DBNComboBoxModel<DBColumn> modelID = sourceColumnIdComboBox.getModel();
        DBNComboBoxModel<DBColumn> modelData = sourceDataColumnComboBox.getModel();
        modelData.removeAllElements();
        modelID.removeAllElements();


        List<DBColumn> columns = table.getColumns();

        Dispatch.run(ModalityState.any() ,()->{
          sourceColumnIdComboBox.clearValues();
          sourceDataColumnComboBox.clearValues();
          addItems(sourceColumnIdComboBox, columns);
          addItems(sourceDataColumnComboBox,columns);
          sourceColumnIdComboBox.setSelectedItem(idColumn);
          sourceDataColumnComboBox.setSelectedItem(dataColumn);

          sourceColumnIdComboBox.revalidate();
          sourceColumnIdComboBox.repaint();
          sourceDataColumnComboBox.revalidate();
          sourceDataColumnComboBox.repaint();
        });



      }finally {
        stopActivityNotifier();
      }
    });

  }

  private void loadSchemas() {
    Background.run(()->{
      try{
        startActivityNotifier();
        DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
        List<DBSchema> schemas = objectBundle.getSchemas();
        DBSchema schema = objectBundle.getUserSchema();
        schemaComboBox.setValues(schemas);
        schemaComboBox.setSelectedItem(schema);
      }finally {
        stopActivityNotifier();
      }
    });
  }

  private void populateDatabseObjectTable(DBSchema schema) {
    if(schema == null) return;
    Background.run(()->{
      startActivityNotifier();
      try{
        DBNComboBoxModel<DBTable> model = sourceTableComboBox.getModel();
        model.removeAllElements();

        List<DBTable> tables = schema.getTables();

        Dispatch.run(ModalityState.any(),()->{
          System.out.println("ggayyyqweereraaa");
          sourceTableComboBox.clearValues();
          addItems(sourceTableComboBox, tables);
          sourceTableComboBox.setSelectedItem(tables.get(0));
          sourceTableComboBox.revalidate();
          sourceTableComboBox.repaint();
        });
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
