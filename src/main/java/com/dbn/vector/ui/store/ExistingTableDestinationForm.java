package com.dbn.vector.ui.store;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.store.StoreConfig;
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

public class ExistingTableDestinationForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JLabel schemaLabel;
  private JLabel tableLabel;
  private JLabel dataColumnLabel;
  private JLabel embeddingColumnLabel;
  private JLabel metadataColumnLabel;
  private DBObjectSelector<DBSchema> schemaComboBox;
  private DBObjectSelector<DBTable> tableComboBox;
  private DBObjectSelector<DBColumn> dataColumnComboBox;
  private DBObjectSelector<DBColumn> embeddingColumnComboBox;
  private DBObjectSelector<DBColumn> metadataColumnComboBox;

  public ExistingTableDestinationForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection) {
    super(parent, connection);
  }

  private void initComboBoxes() {
    StoreConfig config = getConfig();
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

    dataColumnComboBox
            .initialize(this, COLUMN)
            .withConnectionContext(() -> getConnection())
            .withValueLoader(() -> loadDataColumns())
            .withValuePreselector(() -> config.getTextColumnName())
            .triggerLoad();

    embeddingColumnComboBox
            .initialize(this, COLUMN)
            .withConnectionContext(() -> getConnection())
            .withValueLoader(() -> loadEmbeddingColumns())
            .withValuePreselector(() -> config.getEmbeddingColumnName())
            .triggerLoad();

    metadataColumnComboBox
            .initialize(this, COLUMN)
            .withConnectionContext(() -> getConnection())
            .withValueLoader(() -> loadMetadataColumns())
            .withValuePreselector(() -> config.getMetadataColumnName())
            .triggerLoad();
  }

  protected void initEventListeners() {
    onSelectionChange(schemaComboBox, e -> populateTables());
    onSelectionChange(tableComboBox,  e -> populateColumns());
  }

  @Override
  protected void initFieldAvailability() {
    DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(tableComboBox));
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedTable()), array(
            dataColumnComboBox,
            embeddingColumnComboBox,
            metadataColumnComboBox));
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
    alignerData.registerFieldGroup(tableLabel, tableComboBox);
    alignerData.registerFieldGroup(dataColumnLabel, dataColumnComboBox);
    alignerData.registerFieldGroup(embeddingColumnLabel, embeddingColumnComboBox);
    alignerData.registerFieldGroup(metadataColumnLabel, metadataColumnComboBox);
  }

  private List<DBColumn> loadEmbeddingColumns() {
    DBTable table = ComboBoxes.getSelection(tableComboBox);
    List<DBColumn> columns = table == null ?
            Collections.emptyList() :
            table.getColumns();

    return Lists.filter(columns, c -> c.getDataType().getGenericDataType() == GenericDataType.VECTOR);
  }

  private List<DBColumn> loadMetadataColumns() {
    DBTable table = ComboBoxes.getSelection(tableComboBox);
    List<DBColumn> columns = table == null ?
            Collections.emptyList() :
            table.getColumns();

    return Lists.filter(columns, c -> c.getDataType().getGenericDataType() == GenericDataType.JSON);
  }

  private List<DBColumn> loadDataColumns() {
    DBTable table = ComboBoxes.getSelection(tableComboBox);
    List<DBColumn> columns = table == null ?
            Collections.emptyList() :
            table.getColumns();

    return Lists.filter(columns, c -> c.getDataType().isLiteral() && !c.isPrimaryKey() && !c.isHidden());
  }


  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox,"Please select a schema");
    addSelectionValidation(tableComboBox,"Please select a table");
    addSelectionValidation(dataColumnComboBox,"Please select the primary key column");
    addSelectionValidation(embeddingColumnComboBox,"Please select a data column");
    addSelectionValidation(metadataColumnComboBox,"Please select a metadata column");
  }

  private void populateColumns() {
    updateFieldAvailability();
    dataColumnComboBox.reloadValues();
    embeddingColumnComboBox.reloadValues();
    metadataColumnComboBox.reloadValues();
  }

  private void populateTables() {
    updateFieldAvailability();
    tableComboBox.reloadValues();
    dataColumnComboBox.reloadValues();
    embeddingColumnComboBox.reloadValues();
    metadataColumnComboBox.reloadValues();
  }

  @Nullable
  public DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  @Nullable
  public DBTable getSelectedTable() {
    return ComboBoxes.getSelection(tableComboBox);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public void resetFormChanges() {
    initComboBoxes();
  }

  @Override
  public void applyFormChanges() {
    StoreConfig config = getConfig();
    config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
    config.setTableName(getSelectedObjectName(tableComboBox, config.getTableName()));
    config.setTextColumnName(getSelectedObjectName(dataColumnComboBox, config.getTextColumnName()));
    config.setEmbeddingColumnName(getSelectedObjectName(embeddingColumnComboBox, config.getEmbeddingColumnName()));
    config.setMetadataColumnName(getSelectedObjectName(metadataColumnComboBox, config.getMetadataColumnName()));
  }

  public StoreConfig getConfig() {
    return getEmbeddingRequest().getStoreConfig();
  }
}