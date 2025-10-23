package com.dbn.vector.ui.embed;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.embed.ThirdPartyModel;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ThirdPartyModelConfigForm extends DBNFormBase {
  private JPanel mainPanel;
  private JBTextField Provider;
  private JBTextField CredentialName;
  private JBTextField Url;
  private JBTextField ModelName;

  public ThirdPartyModelConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public String getProvider() {
    return Provider.getText();
  }
  public String getCredentialName() {
    return CredentialName.getText();
  }

  public String getUrl() {
    return Url.getText();
  }

  public String getModelName() {
    return ModelName.getText();
  }

  public EmbedConfig getEmbedConfig() {
    ThirdPartyModel embedConfig = new ThirdPartyModel(
            getProvider(),
            getCredentialName(),
            getUrl(),
            getModelName()
    );
    return embedConfig;
  }
}
