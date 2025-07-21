package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SourceDataForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private ComboBox<String> sourceCombo;
  ConnectionHandler connectionHandler;
  private FileSystemSourceForm fileSystemSourceForm;
  private DBTableSourceForm tableSourceForm;

  public SourceDataForm(@Nullable Disposable parent,ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    fileSystemSourceForm = new FileSystemSourceForm(this, connectionHandler);
    tableSourceForm = new DBTableSourceForm(this, connectionHandler);
    JPanel tablePanel = (JPanel) tableSourceForm.getMainComponent();
    dataPanel.setLayout(new BorderLayout());
    dataPanel.add(tablePanel, BorderLayout.CENTER);
  }

  private void initComboBox() {
    sourceCombo.addActionListener(e -> {
      dataPanel.removeAll();
      String source = (String) sourceCombo.getSelectedItem();
      if ("FILESYSTEM".equalsIgnoreCase(source)) {
        dataPanel.add((JPanel) fileSystemSourceForm.getMainComponent(), BorderLayout.CENTER);
      } else if ("TABLE".equalsIgnoreCase(source)) {
        dataPanel.add((JPanel) tableSourceForm.getMainComponent(), BorderLayout.CENTER);
      }
      dataPanel.revalidate();
      dataPanel.repaint();
    });
  }

  public SourceConfig getSourceConfig() {
    if (sourceCombo.getSelectedItem().equals("Filesystem")) {
      return fileSystemSourceForm.getfileSystemSourceConfig();
    }else{
      return tableSourceForm.getDBTableSourceConfig();
    }
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Source data";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Source data";
  }
}
