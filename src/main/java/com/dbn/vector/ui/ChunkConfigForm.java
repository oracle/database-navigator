package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ChunkConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel chunkConfigPanel;
  private JComboBox comboBox4;
  private JSpinner spinner1;
  private JComboBox comboBox5;
  private JSpinner spinner2;
  private JButton chunkLaboButton;

  public ChunkConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);

    System.out.println("VectorAIForm");
    chunkLaboButton.addActionListener(e -> {
      Dialogs.show(()->new ChunkEditorDialog(getProject(),"Chunk Editor",true,connectionHandler));
      System.out.println("hi");
      System.out.println("fjkdsfj");
    });
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Chunk Config";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Chunk Config";
  }
}
