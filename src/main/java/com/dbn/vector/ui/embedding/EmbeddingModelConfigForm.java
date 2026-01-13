package com.dbn.vector.ui.embedding;

import com.dbn.assistant.provider.AIProvider;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Strings;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.vector.model.request.EmbeddingModelConfig;
import com.dbn.vector.model.request.EmbeddingModelLocation;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class EmbeddingModelConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JComboBox<EmbeddingModelLocation> modelTypeComboBox;
  private JPanel configPanel;
  private JLabel modelTypeLabel;
  private EmbeddingModelDatabaseForm databaseModelConfigForm;
  private EmbeddingModelThirdPartyForm thirdPartyModelConfigForm;

  public EmbeddingModelConfigForm(@NotNull VectorToolboxFormBase parent) {
    super(parent);
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
    databaseModelConfigForm = new EmbeddingModelDatabaseForm(this);
    thirdPartyModelConfigForm = new EmbeddingModelThirdPartyForm(this);
    updateConfigForm();
  }


  private void initComboBoxes() {
    initComboBox(modelTypeComboBox, EmbeddingModelLocation.values());
    setSelection(modelTypeComboBox, EmbeddingModelLocation.IN_DATABASE_MODEL);
    onSelectionChange(modelTypeComboBox, v -> updateConfigForm());
  }

  private void updateConfigForm() {
    EmbeddingModelLocation modelLocation = ComboBoxes.getSelection(modelTypeComboBox);
    configPanel.removeAll();
    if (modelLocation == EmbeddingModelLocation.IN_DATABASE_MODEL) {
      configPanel.add(databaseModelConfigForm.getComponent());

    } else if (modelLocation == EmbeddingModelLocation.THIRD_PARTY_MODEL) {
      configPanel.add(thirdPartyModelConfigForm.getComponent());
    }

    configPanel.revalidate();
    configPanel.repaint();
  }

  @Override
  public void resetFormChanges() {
    EmbeddingModelConfig config = getConfig();

    setSelection(modelTypeComboBox, config.getModelLocation());
    databaseModelConfigForm.resetFormChanges();
    thirdPartyModelConfigForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    EmbeddingModelConfig config = getConfig();

    config.setModelLocation(getSelection(modelTypeComboBox));
    databaseModelConfigForm.applyFormChanges();
    thirdPartyModelConfigForm.applyFormChanges();
  }

  public EmbeddingModelConfig getConfig() {
    return getEmbeddingRequest().getModelConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getFormTitle() {
    return "Embedding Model";
  }

  @Override
  public String getFormTitleDetail() {
    EmbeddingModelLocation modelLocation = getSelection(modelTypeComboBox);
    String modelLocationName = modelLocation == null ? "" : modelLocation.getName();
    if (modelLocation == EmbeddingModelLocation.IN_DATABASE_MODEL) {
      DBSchema selectedSchema = databaseModelConfigForm.getSelectedSchema();
      DBAIModel selectedModel = databaseModelConfigForm.getSelectedModel();
      if (selectedModel == null) return modelLocationName;

      return modelLocationName + " - " + selectedSchema + "." + selectedModel;
    }

    if (modelLocation == EmbeddingModelLocation.THIRD_PARTY_MODEL) {
      AIProvider provider = thirdPartyModelConfigForm.getProvider();
      String modelName = thirdPartyModelConfigForm.getModelName();

      if (provider != null && Strings.isNotEmpty(modelName)) {
        return modelLocationName + " - " + provider.getName() + " / " + modelName;
      }

      if (provider != null) return modelLocationName + " - " + provider.getName();
    }

    return modelLocationName;
  }
}
