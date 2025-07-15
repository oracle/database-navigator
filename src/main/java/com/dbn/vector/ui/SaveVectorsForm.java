package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SaveVectorsForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel saveDataPanel;
  private JCheckBox createNewOneCheckBox;
  private JTextField textField1;
  private JTextField textField2;
  private JComboBox comboBox7;
  private JSpinner spinner3;
  private JTextField textField3;
  private JTextField textField4;
  private JSpinner spinner4;

  public SaveVectorsForm(@Nullable Disposable parent) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Save Vectors";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Save Vectors";
  }
}
