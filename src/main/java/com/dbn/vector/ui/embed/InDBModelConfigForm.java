package com.dbn.vector.ui.embed;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.factory.ui.common.ObjectFactoryInputDialog;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.embed.EmbedConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;

public class InDBModelConfigForm extends DBNFormBase {
  private final ConnectionHandler connectionHandler;
  private JPanel mainPanel;
  private DBNComboBox<DBAIModel> modelDBNComboBox;
  private JPanel spinIconPanel;
  private JButton addCredentialButton;

  public InDBModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    spinIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    modelDBNComboBox.set(HIDE_DESCRIPTION, true);
    initModelAddButton();
    loadAiModels();

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

      loadAiModels();
    });
  }

  private void loadAiModels() {
    Background.run(()->{
      try {
        startActivityNotifier();
        DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
        DBSchema schema = objectBundle.getUserSchema();

        List<DBAIModel> models =schema.getAiModels();
        modelDBNComboBox.setValues(models);
        if (!models.isEmpty()) modelDBNComboBox.setSelectedIndex(0);
      }finally {
        stopActivityNotifier();
      }
    });
  }

  private void startActivityNotifier() {
    spinIconPanel.setVisible(true);
  }

  /**
   * Stops the spining wheel
   */
  private void stopActivityNotifier() {
    spinIconPanel.setVisible(false);
  }

  public EmbedConfig getEmbedConfig() {
    EmbedConfig embedConfig = new EmbedConfig();
    embedConfig.setModelName(((DBAIModel)modelDBNComboBox.getSelectedItem()).getName());
    return embedConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
