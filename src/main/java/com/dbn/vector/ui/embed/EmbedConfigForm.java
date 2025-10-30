package com.dbn.vector.ui.embed;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.embed.ModelLocation;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class EmbedConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JComboBox<ModelLocation> modelTypeComboBox;
  private JPanel configPanel;
  private JLabel modelTypeLabel;
  private InDBModelConfigForm databaseModelConfigForm;
  private ThirdPartyModelConfigForm thirdPartyModelConfigForm;

  public EmbedConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    initComboBoxes();
    initDataPanel();
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(modelTypeLabel, modelTypeComboBox);
    alignerData.registerForms(databaseModelConfigForm, thirdPartyModelConfigForm);
  }

  private void initDataPanel() {
    ConnectionHandler connection = getConnection();
    databaseModelConfigForm = new InDBModelConfigForm(this, connection);
    thirdPartyModelConfigForm = new ThirdPartyModelConfigForm(this, connection);
    updateConfigForm();
  }


  private void initComboBoxes() {
    initComboBox(modelTypeComboBox, ModelLocation.values());
    setSelection(modelTypeComboBox, ModelLocation.IN_DATABASE_MODEL);
    onSelectionChange(modelTypeComboBox, v -> updateConfigForm());
  }

  private void updateConfigForm() {
    ModelLocation modelLocation = ComboBoxes.getSelection(modelTypeComboBox);
    configPanel.removeAll();
    if (modelLocation == ModelLocation.IN_DATABASE_MODEL) {
      configPanel.add(databaseModelConfigForm.getComponent());

    } else if (modelLocation == ModelLocation.THIRD_PARTY_MODEL) {
      configPanel.add(thirdPartyModelConfigForm.getComponent());
    }

    configPanel.revalidate();
    configPanel.repaint();
  }

  @Override
  public void resetFormChanges() {
    EmbedConfig config = getConfig();

    setSelection(modelTypeComboBox, config.getModelLocation());
    databaseModelConfigForm.resetFormChanges();
    thirdPartyModelConfigForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    EmbedConfig config = getConfig();

    config.setModelLocation(getSelection(modelTypeComboBox));
    databaseModelConfigForm.applyFormChanges();
    thirdPartyModelConfigForm.applyFormChanges();
  }

  public EmbedConfig getConfig() {
    return getEmbeddingRequest().getEmbedConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Embedding Model";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Embedding Model";
  }

}
