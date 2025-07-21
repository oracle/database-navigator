package com.dbn.vector.ui.embed;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ThirdPartyModelConfigForm extends DBNFormBase {
  private JPanel mainPanel;
  public ThirdPartyModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
