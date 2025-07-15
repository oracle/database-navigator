package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EmbedConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel embedConfigPanel;
  private JRadioButton inDatabaseModelRadioButton;
  private JRadioButton thirdpartyModelsRadioButton;
  private JComboBox comboBox6;

  public EmbedConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Embed Config";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Embed Config";
  }
}
