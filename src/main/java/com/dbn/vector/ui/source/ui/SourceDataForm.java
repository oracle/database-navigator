package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.sourceconfig.SourceType;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class SourceDataForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<SourceType> sourceComboBox;
  private JLabel sourceLabel;
  private FileSystemSourceForm fileSourceForm;
  private DBTableSourceForm tableSourceForm;

  public SourceDataForm(@Nullable Disposable parent,ConnectionHandler connection) {
    super(parent, connection);
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    ConnectionHandler connection = getConnection();
    fileSourceForm = new FileSystemSourceForm(this, connection);
    tableSourceForm = new DBTableSourceForm(this, connection);
    updateSourceForm();
  }

  private void initComboBox() {
    ComboBoxes.initComboBox(sourceComboBox, SourceType.values());
    setSelection(sourceComboBox, SourceType.DATABASE_TABLE);
  }

  @Override
  protected void initEventListeners() {
    onSelectionChange(sourceComboBox, t -> updateSourceForm());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(sourceLabel, sourceComboBox);
    alignerData.registerForms(tableSourceForm);
  }

  private void updateSourceForm() {
    SourceType sourceType = getSelectedSourceType();
    dataPanel.removeAll();
    if (sourceType == SourceType.FILE_SYSTEM) {
      dataPanel.add(fileSourceForm.getComponent());
    } else if (sourceType == SourceType.DATABASE_TABLE) {
      dataPanel.add(tableSourceForm.getComponent());
    }
    dataPanel.revalidate();
    dataPanel.repaint();
    validateFormFields();
  }

  @Override
  public void resetFormChanges() {
    SourceConfig config = getConfig();

    setSelection(sourceComboBox, config.getSourceType());
    tableSourceForm.resetFormChanges();
    fileSourceForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    SourceConfig config = getConfig();

    config.setSourceType(getSelectedSourceType());
    tableSourceForm.applyFormChanges();
    fileSourceForm.applyFormChanges();
  }

  public SourceConfig getConfig() {
    return getEmbeddingRequest().getSourceConfig();
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
      return getSelectedSourceType().getName();
  }

  @Override
  public String getExpandedTitle() {
    return "Data Source";
  }

  public SourceType getSelectedSourceType() {
    return ComboBoxes.getSelection(sourceComboBox);
  }


}
