package com.dbn.vector.ui.embed;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.factory.ui.common.ObjectFactoryInputDialog;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.embed.InDBModel;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;

public class InDBModelConfigForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private DBNComboBox<DBAIModel> modelComboBox;
  private DBNComboBox<DBSchema> schemaComboBox;
  private JButton addCredentialButton;
  private JLabel schemaLabel;
  private JLabel modelLabel;

  public InDBModelConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    modelComboBox.set(HIDE_DESCRIPTION, true);
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    initComboboxListeners();
    initModelAddButton();
    initComboBoxes();
  }
  private void initModelAddButton() {
    addCredentialButton.setIcon(Icons.ACTION_ADD);
    addCredentialButton.setText(null);
    System.out.println("kl");
    ConnectionHandler connection = getConnection();
    addCredentialButton.addActionListener(e -> Dialogs.show(() -> new ObjectFactoryInputDialog(getProject(), connection.getSchema(connection.getUserSchema()),DBObjectType.AI_MODEL)));

    Project project = connection.getProject();
    ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, e -> {
      if (!e.matches(connection)) return;
      if (!e.matches(DBObjectType.AI_MODEL)) return;
      modelComboBox.reloadValues();
    });
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
    fieldAdapter.initFieldsVisibility(() -> isValid(getSelectedSchema()), array(addCredentialButton));
  }

  private List<DBSchema> loadSchemas() {
    DBObjectBundle objectBundle = getConnection().getObjectBundle();
    return objectBundle.getSchemas();
  }

  private List<DBAIModel> loadModels() {
    DBSchema schema = getSelectedSchema();
    if (schema == null) return java.util.Collections.emptyList();
    return schema.getAiModels();

  }

  private @Nullable DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  private void initComboBoxes() {
    // TODO add value preselectors when restoring the screen state
    schemaComboBox.init(() -> loadSchemas(), null);
    modelComboBox.init(() -> loadModels(), null);
  }

  private void initComboboxListeners() {
    schemaComboBox.addListener((ov, nv) -> populateModels());
    modelComboBox.set(HIDE_DESCRIPTION, true);
  }

  private void populateModels() {
    updateFieldAvailability();
    modelComboBox.reloadValues();
  }


  public EmbedConfig getEmbedConfig() {
    // get name as Schema.modelName
    InDBModel embedConfig = new InDBModel(((DBAIModel) Objects.requireNonNull(modelComboBox.getSelectedItem())).getName());
//    embedConfig.setModelName(((DBAIModel)modelDBNComboBox.getSelectedItem()).getName());
    return embedConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
