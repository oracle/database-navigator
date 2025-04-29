package com.dbn.object.common;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DCNConfigForm extends DBNFormBase {
  private JPanel mainPanel;
  private final DBObjectRef<DBTable> object;
  private JCheckBox insertCheckBox;
  private JCheckBox updateCheckBox;
  private JCheckBox deleteCheckBox;


  public DCNConfigForm(@Nullable Disposable parent, final DBTable dbTable) {
    super(parent);
    this.object = DBObjectRef.of(dbTable);

  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
