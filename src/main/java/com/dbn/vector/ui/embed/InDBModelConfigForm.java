package com.dbn.vector.ui.embed;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;

public class InDBModelConfigForm extends DBNFormBase {
  private final ConnectionHandler connectionHandler;
  private JPanel mainPanel;
  private DBNComboBox<DBAIModel> modelDBNComboBox;
  private JButton addCredentialButton;
  private DBNComboBox<DBSchema> schemaDBNComboBox;

  public InDBModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    modelDBNComboBox.set(HIDE_DESCRIPTION, true);
    schemaDBNComboBox.set(HIDE_DESCRIPTION, true);
    initComboboxListeners();
    initModelAddButton();
    initComboBoxes();
  }
  private void initModelAddButton() {
    addCredentialButton.setIcon(Icons.ACTION_ADD);
    addCredentialButton.setText(null);
    System.out.println("kl");
    ConnectionHandler connection = connectionHandler;
    addCredentialButton.addActionListener(e -> Dialogs.show(() -> new ObjectFactoryInputDialog(getProject(), connection.getSchema(connection.getUserSchema()),DBObjectType.AI_MODEL)));

    Project project = connection.getProject();
    ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, e -> {
      if (!e.matches(connection)) return;
      if (!e.matches(DBObjectType.AI_MODEL)) return;
      modelDBNComboBox.reloadValues();
    });
  }


  private List<DBSchema> loadSchemas() {
    DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
    return objectBundle.getSchemas();
  }

  private List<DBAIModel> loadAiModelsForSelectedSchema() {
    DBSchema schema = ComboBoxes.getSelection(schemaDBNComboBox);
    if (schema == null) return java.util.Collections.emptyList();
    return schema.getAiModels();

  }

  private void initComboBoxes() {
    schemaDBNComboBox.setValueLoader(this::loadSchemas);
    modelDBNComboBox.setValueLoader(this::loadAiModelsForSelectedSchema);
    schemaDBNComboBox.loadValues();
    modelDBNComboBox.loadValues();
  }

  private void initComboboxListeners() {
    schemaDBNComboBox.addListener((ov, nv) -> modelDBNComboBox.reloadValues());
    modelDBNComboBox.set(HIDE_DESCRIPTION, true);
  }


  public EmbedConfig getEmbedConfig() {
    // get name as Schema.modelName
    InDBModel embedConfig = new InDBModel(((DBAIModel) Objects.requireNonNull(modelDBNComboBox.getSelectedItem())).getName());
//    embedConfig.setModelName(((DBAIModel)modelDBNComboBox.getSelectedItem()).getName());
    return embedConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
