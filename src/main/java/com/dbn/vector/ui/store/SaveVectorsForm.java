package com.dbn.vector.ui.store;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SaveVectorsForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel saveDataPanel;
  private JTextField textField1;
  private JTextField embeddingsVECTORTextField;
  private JTextField contentCLOBTextField;

  public SaveVectorsForm(@Nullable Disposable parent) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public StoreConfig getStoreConfig() {
    StoreConfig storeConfig =  new StoreConfig();
    storeConfig.setTableName(textField1.getText());
    return storeConfig;
  }
  @Override
  public String getCollapsedTitle() {
    return "Embedding Destination";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Embedding Destination";
  }
}
