package com.dbn.vector.ui.embed;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.embed.EmbedConfig;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class EmbedConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JComboBox<ModelLocation> modelTypeComboBox;
  private JPanel configPanel;
  private JLabel modelTypeLabel;
  private ConnectionHandler connectionHandler;
  private InDBModelConfigForm databaseModelConfigForm;
  private ThirdPartyModelConfigForm thirdPartyModelConfigForm;

  public EmbedConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    initComboBox();
    initDataPanel();
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(modelTypeLabel, modelTypeComboBox);
    alignerData.registerForms(databaseModelConfigForm, thirdPartyModelConfigForm);
  }

  private void initDataPanel() {
    databaseModelConfigForm = new InDBModelConfigForm(this,connectionHandler);
    thirdPartyModelConfigForm = new ThirdPartyModelConfigForm(this,connectionHandler);
    updateConfigForm();
  }


  private void initComboBox() {
    ComboBoxes.initComboBox(modelTypeComboBox, ModelLocation.values());
    ComboBoxes.setSelection(modelTypeComboBox, ModelLocation.IN_DATABASE_MODEL);
    ComboBoxes.onSelectionChange(modelTypeComboBox, v -> updateConfigForm());
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

  public EmbedConfig getEmbedConfig() {
    ModelLocation modelLocation = ComboBoxes.getSelection(modelTypeComboBox);
    if (modelLocation == null) return null;

    switch (modelLocation) {
      case IN_DATABASE_MODEL: return databaseModelConfigForm.getEmbedConfig();
      case THIRD_PARTY_MODEL: return thirdPartyModelConfigForm.getEmbedConfig();
      default:
        throw new IllegalStateException("Unexpected value: " + modelLocation);
    }
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

  @Getter
  public enum ModelLocation implements Presentable {
    IN_DATABASE_MODEL("In-database model"),
    THIRD_PARTY_MODEL("Third-party model");
    private final String name;
    ModelLocation(String name) { this.name = name; }
  }
}
