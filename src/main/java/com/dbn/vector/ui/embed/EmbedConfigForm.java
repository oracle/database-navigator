package com.dbn.vector.ui.embed;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.embed.EmbedConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class EmbedConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel embedConfigPanel;
  private JComboBox modelTypeComboBox;
  private JPanel configPanel;
  private ConnectionHandler connectionHandler;
  private InDBModelConfigForm InDBModelConfigForm;
  private ThirdPartyModelConfigForm thirdPartyModelConfigForm;

  public EmbedConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    initDataPanel();
    initComboBox();
  }

  private void initDataPanel() {
    InDBModelConfigForm = new InDBModelConfigForm(this,connectionHandler);
    thirdPartyModelConfigForm = new ThirdPartyModelConfigForm(this,connectionHandler);

    JPanel inDBModelConfigForm = (JPanel) InDBModelConfigForm.getMainComponent();
    JPanel thirdPartyModelConfigFormPanel = (JPanel) thirdPartyModelConfigForm.getMainComponent();

    configPanel.add(inDBModelConfigForm,"In_Database_Model");
    configPanel.add(thirdPartyModelConfigFormPanel,"Third_Party_Model");
    CardLayout cardLayout = (CardLayout) configPanel.getLayout();

    // default with table

    cardLayout.show(configPanel, "In_Database_Model");
  }


  private void initComboBox() {

    modelTypeComboBox.addActionListener(e -> {
      CardLayout cardLayout = (CardLayout) configPanel.getLayout();
      String source = (String) modelTypeComboBox.getSelectedItem();
      if (source != null) {
        if (source.equalsIgnoreCase("In Database Model")) {
          cardLayout.show(configPanel, "In_Database_Model");
        }
        else if (source.equalsIgnoreCase("Third Party Model")) {
          cardLayout.show(configPanel, "Third_Party_Model");
        }
      }
    });
  }

  public EmbedConfig getEmbedConfig() {
    return InDBModelConfigForm.getEmbedConfig();
  }
  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Embed Config";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Embed Config";
  }
}
