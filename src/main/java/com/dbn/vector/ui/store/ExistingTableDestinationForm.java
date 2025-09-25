package com.dbn.vector.ui.store;

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
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.resetComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.data.type.GenericDataType.LITERAL;

public class ExistingTableDestinationForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private JPanel initializingIconPanel;
  private DBNComboBox<DBColumn> destinationEmbeddingComboBox;
  private DBNComboBox<DBTable> destinationTableComboBox;
  private DBNComboBox<DBColumn> destinationDataColumnComboBox;

  private ConnectionHandler connectionHandler;



  public ExistingTableDestinationForm(@Nullable Disposable parent, @Nullable ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;

    if (initializingIconPanel != null) {
      initializingIconPanel.setLayout(new BorderLayout());
      initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    }

    initComboboxListeners();
    whenShown(() -> populateSchemas());
  }

  private void initComboboxListeners() {
    if (schemaComboBox != null) schemaComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    if (destinationTableComboBox != null) destinationTableComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
    if (destinationDataColumnComboBox != null) destinationDataColumnComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);

    if (schemaComboBox != null) {
      schemaComboBox.addListener((ov, nv) -> populateTables((DBSchema) nv));
    }
    if (destinationTableComboBox != null) {
      destinationTableComboBox.addListener((ov, nv) -> populateColumns((DBTable) nv));
    }
  }

  private void populateSchemas() {
    resetComboBox(schemaComboBox);
    if (connectionHandler == null) return;

    Background.run(() -> {
      try {
        startActivityNotifier();
        DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
        List<DBSchema> schemas = objectBundle.getSchemas();
        DBSchema userSchema = objectBundle.getUserSchema();

        Dispatch.run(mainPanel, () -> {
          initComboBox(schemaComboBox, schemas);
          setSelection(schemaComboBox, userSchema);
        });
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void populateTables(DBSchema schema) {
    resetComboBox(destinationTableComboBox);
    resetComboBox(destinationDataColumnComboBox);
    if (schema == null) return;

    Background.run(() -> {
      try {
        startActivityNotifier();
        List<DBTable> tables = schema.getTables();
        Dispatch.run(mainPanel, () -> initComboBox(destinationTableComboBox, tables));
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void populateColumns(DBTable table) {
    resetComboBox(destinationDataColumnComboBox);
    resetComboBox(destinationEmbeddingComboBox);
    if (table == null) return;

    Background.run(() -> {
      try {
        startActivityNotifier();
        List<DBColumn> columns = table.getColumns();

        // choose first native literal column as default data column
        DBColumn dataColumn = Lists.first(columns, c ->
                c.getDataType().isNative() &&
                        c.getDataType().getGenericDataType().is(LITERAL));

        Dispatch.run(mainPanel, () -> {
          initComboBox(destinationDataColumnComboBox, columns);
          setSelection(destinationDataColumnComboBox, dataColumn);
          initComboBox(destinationEmbeddingComboBox, columns);
        });
      } finally {
        stopActivityNotifier();
      }
    });
  }

  private void startActivityNotifier() {
    if (initializingIconPanel != null) initializingIconPanel.setVisible(true);
  }

  private void stopActivityNotifier() {
    if (initializingIconPanel != null) initializingIconPanel.setVisible(false);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public StoreConfig toStoreConfig() {
    StoreConfig storeConfig = new StoreConfig();


    DBTable table = destinationTableComboBox == null ? null : (DBTable) destinationTableComboBox.getSelectedValue();
    DBColumn dataCol = destinationDataColumnComboBox == null ? null : (DBColumn) destinationDataColumnComboBox.getSelectedValue();
    DBColumn embeddingCol = destinationEmbeddingComboBox == null ? null : destinationEmbeddingComboBox.getSelectedValue();

    storeConfig.setTableName(table.getName());
    storeConfig.setEmbeddingColumn(embeddingCol.getName());
    storeConfig.setTextColumn(dataCol.getName());
    return storeConfig;
  }
}