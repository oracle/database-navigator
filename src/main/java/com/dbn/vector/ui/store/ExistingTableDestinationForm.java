package com.dbn.vector.ui.store;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
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
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;

public class ExistingTableDestinationForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBSchema> schemaComboBox;
  private DBNComboBox<DBTable> tableComboBox;
  private DBNComboBox<DBColumn> dataColumnComboBox;
  private DBNComboBox<DBColumn> embeddingColumnComboBox;
  private DBNComboBox<DBColumn> metadataColumnComboBox;
  private JLabel schemaLabel;
  private JLabel tableLabel;
  private JLabel dataColumnLabel;
  private JLabel embeddingColumnLabel;
  private JLabel metadataColumnLabel;

  public ExistingTableDestinationForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection) {
    super(parent, connection);
    initComboBoxes();
  }

  private void initComboBoxes() {
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    tableComboBox.set(HIDE_DESCRIPTION, true);
    dataColumnComboBox.set(HIDE_DESCRIPTION, true);
    embeddingColumnComboBox.set(HIDE_DESCRIPTION, true);
    metadataColumnComboBox.set(HIDE_DESCRIPTION, true);

    schemaComboBox.init(() -> loadSchemas(), null);
    tableComboBox.init(() -> loadTables(), null);
    dataColumnComboBox.init(() -> loadDataColumns(), null);
    embeddingColumnComboBox.init(() -> loadEmbeddingColumns(), null);
    metadataColumnComboBox.init(() -> loadMetadataColumns(), null);

    updateFieldAvailability();
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
  protected DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  @Nullable
  private DBTable getSelectedTable() {
    return ComboBoxes.getSelection(tableComboBox);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public void resetFormChanges() {
    StoreConfig config = getConfig();
    schemaComboBox.init(() -> loadSchemas(), o -> matchesObjectName(o, config.getSchemaName()));
    tableComboBox.init(() -> loadTables(), o -> matchesObjectName(o, config.getTableName()));
    dataColumnComboBox.init(() -> loadDataColumns(), o -> matchesObjectName(o, config.getTextColumnName()));
    embeddingColumnComboBox.init(() -> loadEmbeddingColumns(), o -> matchesObjectName(o, config.getEmbeddingColumnName()));
    metadataColumnComboBox.init(() -> loadMetadataColumns(), o -> matchesObjectName(o, config.getMetadataColumnName()));
  }

  @Override
  public void applyFormChanges() {
    StoreConfig config = getConfig();
    config.setSchemaName(getSelectedObjectName(schemaComboBox));
    config.setTableName(getSelectedObjectName(tableComboBox));
    config.setTextColumnName(getSelectedObjectName(dataColumnComboBox));
    config.setEmbeddingColumnName(getSelectedObjectName(embeddingColumnComboBox));
    config.setMetadataColumnName(getSelectedObjectName(metadataColumnComboBox));
  }

  public StoreConfig getConfig() {
    return getEmbeddingRequest().getStoreConfig();
  }
}