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
  private ComboBox<SourceType> sourceCombo;
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
    dataPanel.setLayout(new BorderLayout());
    sourceCombo.setSelectedItem(SourceType.TABLE);
    SourceType initial = (SourceType) sourceCombo.getSelectedItem();
    JPanel initialPanel = initial == SourceType.FILESYSTEM
        ? (JPanel) fileSystemSourceForm.getMainComponent()
        : (JPanel) tableSourceForm.getMainComponent();
    dataPanel.add(initialPanel, BorderLayout.CENTER);
  }

  private void initComboBox() {
    sourceCombo.setModel(new DefaultComboBoxModel<>(SourceType.values()));
    sourceCombo.addActionListener(e -> {
      dataPanel.removeAll();
      SourceType source = (SourceType) sourceCombo.getSelectedItem();
      switch (source) {
        case FILESYSTEM :
          dataPanel.add((JPanel) fileSystemSourceForm.getMainComponent(), BorderLayout.CENTER);
          break;
        case TABLE :
          dataPanel.add((JPanel) tableSourceForm.getMainComponent(), BorderLayout.CENTER);
      }
      dataPanel.revalidate();
      dataPanel.repaint();
    });
  }

  public SourceConfig getSourceConfig() {
    return sourceCombo.getSelectedItem() == SourceType.FILESYSTEM
        ? fileSystemSourceForm.getFileSystemSourceConfig()
        : tableSourceForm.getDBTableSourceConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Data Source";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Data Source";
  }

  public SourceType getSourceType() {
    return (SourceType) sourceCombo.getSelectedItem();
  }

  public enum SourceType {
    FILESYSTEM("Filesystem"),
    TABLE("Database table");
    private final String label;
    SourceType(String label) { this.label = label; }
    @Override public String toString() { return label; }
  }


}
