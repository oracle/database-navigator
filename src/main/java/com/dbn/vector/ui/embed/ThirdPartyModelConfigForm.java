package com.dbn.vector.ui.embed;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.embed.ThirdPartyModel;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ThirdPartyModelConfigForm extends DBNFormBase {
  private JPanel mainPanel;
  private JBTextField providerTextField;
  private JBTextField credentialTextField;
  private JBTextField urlTextField;
  private JBTextField modelTextField;
  private JLabel providerLabel;
  private JLabel credentialLabel;
  private JLabel urlLabel;
  private JLabel modelLabel;

  public ThirdPartyModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public String getProvider() {
    return providerTextField.getText();
  }

  public String getCredentialName() {
    return credentialTextField.getText();
  }

  public String getUrl() {
    return urlTextField.getText();
  }

  public String getModelName() {
    return modelTextField.getText();
  }

  public EmbedConfig getEmbedConfig() {
      return new ThirdPartyModel(
              getProvider(),
              getCredentialName(),
              getUrl(),
              getModelName()
      );
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(providerLabel, providerTextField);
    alignerData.registerFieldGroup(credentialLabel, credentialTextField);
    alignerData.registerFieldGroup(urlLabel, urlTextField);
    alignerData.registerFieldGroup(modelLabel, modelTextField);
  }
}
