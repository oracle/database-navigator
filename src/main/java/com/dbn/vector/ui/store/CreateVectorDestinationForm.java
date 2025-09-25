package com.dbn.vector.ui.store;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CreateVectorDestinationForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel saveDataPanel;
  private JTextField textField1;
  private JTextField embeddingVECTORTextField;
  private JTextField textCLOBTextField;
  public CreateVectorDestinationForm(@Nullable Disposable parent) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public StoreConfig toStoreConfig() {
    StoreConfig storeConfig = new StoreConfig();

    String tableName = textField1.getText() == null ? "" : textField1.getText().trim();
//    String embedCol  = embeddingsVECTORTextField.getText() == null ? "" : embeddingsVECTORTextField.getText().trim();
//    String dataCol   = contentCLOBTextField.getText() == null ? "" : contentCLOBTextField.getText().trim();
    String embedCol = "embedding";
    String dataCol = "text";
    storeConfig.setTableName(tableName);
    storeConfig.setEmbeddingColumn(embedCol);
    storeConfig.setTextColumn(dataCol);
    return storeConfig;
  }
}
