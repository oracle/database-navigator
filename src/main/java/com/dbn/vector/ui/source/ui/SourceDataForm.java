package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class SourceDataForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<SourceType> sourceComboBox;
  private FileSystemSourceForm fileSystemSourceForm;
  private DBTableSourceForm tableSourceForm;
  private final ConnectionRef connection;

  public SourceDataForm(@Nullable Disposable parent,ConnectionHandler connection) {
    super(parent);
    this.connection = connection.ref();
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    ConnectionHandler connection = getConnection();
    fileSystemSourceForm = new FileSystemSourceForm(this);
    tableSourceForm = new DBTableSourceForm(this, connection);
    updateSourceForm();
  }

  private void initComboBox() {
    ComboBoxes.initComboBox(sourceComboBox, SourceType.values());
    setSelection(sourceComboBox, SourceType.TABLE);
    sourceComboBox.addActionListener(e -> updateSourceForm());
  }

  private void updateSourceForm() {
    SourceType sourceType = getSourceType();
    dataPanel.removeAll();
    if (sourceType == SourceType.FILESYSTEM) {
      dataPanel.add(fileSystemSourceForm.getComponent());
    } else if (sourceType == SourceType.TABLE) {
      dataPanel.add(tableSourceForm.getComponent());
    }
    dataPanel.revalidate();
    dataPanel.repaint();
  }

  public SourceConfig getSourceConfig() {
    return sourceComboBox.getSelectedItem() == SourceType.FILESYSTEM
        ? fileSystemSourceForm.getFileSystemSourceConfig()
        : tableSourceForm.getConfiguration();
  }

  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Data Source:";
  }

  @Override
  public String getCollapsedTitleDetail() {
      return getSourceType().getName();
  }

  @Override
  public String getExpandedTitle() {
    return "Data Source";
  }

  public SourceType getSourceType() {
    return ComboBoxes.getSelection(sourceComboBox);
  }

  @Getter
  public enum SourceType implements Presentable {
    FILESYSTEM("File system"),
    TABLE("Database table");
    private final String name;
    SourceType(String name) { this.name = name; }
  }


}
