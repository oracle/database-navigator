package com.dbn.vector.ui.store;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;

public class ExistingTableDestinationForm extends DBNFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private DBNComboBox<DBTable> tableComboBox;
  private DBNComboBox<DBColumn> embeddingColumnComboBox;
  private DBNComboBox<DBColumn> dataColumnComboBox;
  private JLabel schemaLabel;
  private JLabel tableLabel;
  private JLabel dataColumnLabel;
  private JLabel embeddingColumnLabel;

  private final ConnectionRef connection;

  public ExistingTableDestinationForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection) {
    super(parent);
    this.connection = connection.ref();

    initComboboxListeners();
    initValidation();

    whenShown(() -> initComboBoxes());
  }

  private void initComboBoxes() {
    // TODO add value preselectors when restoring the screen state
    schemaComboBox.init(() -> loadSchemas(), null);
    tableComboBox.init(() -> loadTables(), null);
    embeddingColumnComboBox.init(() -> loadEmbeddingColumns(), null);
    dataColumnComboBox.init(() -> loadDataColumns(), null);
  }

  @Override
  protected void initFieldAvailability() {
    DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(tableComboBox));
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTable()), array(
            embeddingColumnComboBox,
            dataColumnComboBox));
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
    alignerData.registerFieldGroup(tableLabel, tableComboBox);
    alignerData.registerFieldGroup(dataColumnLabel, dataColumnComboBox);
    alignerData.registerFieldGroup(embeddingColumnLabel, embeddingColumnComboBox);
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


  private List<DBColumn> loadEmbeddingColumns() {
    DBTable table = ComboBoxes.getSelection(tableComboBox);
    List<DBColumn> columns = table == null ?
            Collections.emptyList() :
            table.getColumns();

    return Lists.filter(columns, c -> c.getDataType().getGenericDataType() == GenericDataType.VECTOR);
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
    embeddingColumnComboBox.set(HIDE_DESCRIPTION, true);

    schemaComboBox.addListener((ov,nv)-> populateTables());
    tableComboBox.addListener((ov,nv)-> populateColumns());
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(tableComboBox,"Please select a table");
    addSelectionValidation(dataColumnComboBox,"Please select the primary key column");
    addSelectionValidation(embeddingColumnComboBox,"Please select a data column");
  }

  private void populateColumns() {
    updateFieldAvailability();
    embeddingColumnComboBox.reloadValues();
    dataColumnComboBox.reloadValues();
  }

  private void populateTables() {
    updateFieldAvailability();
    tableComboBox.reloadValues();
    embeddingColumnComboBox.reloadValues();
    dataColumnComboBox.reloadValues();
  }

  @Nullable
  private DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  @Nullable
  private DBTable getSelectedTable() {
    return ComboBoxes.getSelection(tableComboBox);
  }

  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public StoreConfig toStoreConfig() {
    StoreConfig storeConfig = new StoreConfig();


    DBTable table = tableComboBox == null ? null : tableComboBox.getSelectedValue();
    DBColumn dataCol = dataColumnComboBox == null ? null : dataColumnComboBox.getSelectedValue();
    DBColumn embeddingCol = embeddingColumnComboBox == null ? null : embeddingColumnComboBox.getSelectedValue();

    storeConfig.setTableName(table.getName());
    storeConfig.setEmbeddingColumn(embeddingCol.getName());
    storeConfig.setTextColumn(dataCol.getName());
    return storeConfig;
  }
}