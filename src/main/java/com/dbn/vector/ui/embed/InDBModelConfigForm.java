package com.dbn.vector.ui.embed;

import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.vector.model.embed.EmbedConfig;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;

public class InDBModelConfigForm extends DBNFormBase {
  private final ConnectionHandler connectionHandler;
  private JPanel mainPanel;
  private DBNComboBox<DBAIModel> modelDBNComboBox;
  private JPanel spinIconPanel;

  public InDBModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    spinIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    modelDBNComboBox.set(HIDE_DESCRIPTION, true);
    loadAiModels();
  }

  private void loadAiModels() {
    Background.run(()->{
      try {
        startActivityNotifier();
        DBObjectBundle objectBundle = connectionHandler.getObjectBundle();
        DBSchema schema = objectBundle.getUserSchema();

        List<DBAIModel> models =schema.getAiModels();
        modelDBNComboBox.setValues(models);
        modelDBNComboBox.setSelectedIndex(0);
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
