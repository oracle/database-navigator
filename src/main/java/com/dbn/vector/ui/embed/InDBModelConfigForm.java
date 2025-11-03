package com.dbn.vector.ui.embed;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ui.common.ObjectFactoryInputDialog;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.embed.DatabaseModelConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static java.util.Collections.emptyList;

public class InDBModelConfigForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBAIModel> modelComboBox;
  private DBNComboBox<DBSchema> schemaComboBox;
  private JButton addModelButton;
  private JLabel schemaLabel;
  private JLabel modelLabel;

  public InDBModelConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    modelComboBox.set(HIDE_DESCRIPTION, true);
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    initModelAddButton();
    initComboBoxes();
  }

  private void initModelAddButton() {
    addModelButton.setIcon(Icons.ACTION_ADD);
    addModelButton.setText(null);
    addModelButton.addActionListener(e -> Dialogs.show(() ->
            new ObjectFactoryInputDialog(
                    ensureProject(),
                    getSelectedSchema(),
                    DBObjectType.AI_MODEL)));

    ObjectChangeEvent.subscribe(this,
            getConnection(),
            DBObjectType.AI_MODEL,
            () -> modelComboBox.reloadValues());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
    alignerData.registerFieldGroup(modelLabel, modelComboBox);
  }

  @Override
  protected void initFieldAvailability() {
    DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(modelComboBox));
    fieldAdapter.initFieldsVisibility(() -> isValid(getSelectedSchema()), array(addModelButton));
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(schemaComboBox, "Please select a schema");
    addSelectionValidation(modelComboBox, "Please select or create a model");
  }

  private List<DBAIModel> loadModels() {
    DBSchema schema = getSelectedSchema();
    if (schema == null) return emptyList();

    return schema.getAIModels();
  }

  @Nullable
  public DBSchema getSelectedSchema() {
    return getSelection(schemaComboBox);
  }

  @Nullable
  public DBAIModel getSelectedModel() {
    return getSelection(modelComboBox);
  }

  private void initComboBoxes() {
    modelComboBox.set(HIDE_DESCRIPTION, true);
    schemaComboBox.set(HIDE_DESCRIPTION, true);

    schemaComboBox.init(() -> loadSchemas(), null);
    modelComboBox.init(() -> loadModels(), null);

    updateFieldAvailability();
  }

  @Override
  protected void initEventListeners() {
    onSelectionChange(schemaComboBox, s -> populateModels());
  }

  @Override
  public void resetFormChanges() {
    DatabaseModelConfig config = getConfig();
    schemaComboBox.init(() -> loadSchemas(), s -> matchesObjectName(s, config.getSchemaName()));
    modelComboBox.init(() -> loadModels(), m -> matchesObjectName(m, config.getModelName()));
  }

  @Override
  public void applyFormChanges() {
    DatabaseModelConfig config = getConfig();
    config.setSchemaName(getSelectedObjectName(schemaComboBox));
    config.setModelName(getSelectedObjectName(modelComboBox));
  }

  private void populateModels() {
    updateFieldAvailability();
    modelComboBox.reloadValues();
  }


  public DatabaseModelConfig getConfig() {
    return getEmbeddingRequest().getEmbedConfig().getDatabaseModelConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
