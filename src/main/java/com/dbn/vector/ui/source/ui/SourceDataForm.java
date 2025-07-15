package com.dbn.vector.ui.source.ui;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SourceDataForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private ComboBox sourceCombo;
  ConnectionHandler connectionHandler;

  public SourceDataForm(@Nullable Disposable parent,ConnectionHandler connectionHandler) {
    super(parent);
    this.connectionHandler = connectionHandler;
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    JPanel fileSystemPanel = (JPanel) new FileSystemSourceForm(this,connectionHandler).getMainComponent();
    JPanel tablePanel = (JPanel) new DBTableSourceForm(this,connectionHandler).getMainComponent();

    dataPanel.add(fileSystemPanel,"FILESYSTEM");
    dataPanel.add(tablePanel,"TABLE");
    CardLayout cardLayout = (CardLayout) dataPanel.getLayout();

    // default with table

    cardLayout.show(dataPanel, "TABLE");
  }

  private void initComboBox() {
    sourceCombo.addActionListener(e -> {
      CardLayout cardLayout = (CardLayout) dataPanel.getLayout();
      String source = (String) sourceCombo.getSelectedItem();
      if (source != null) {
        if (source.equalsIgnoreCase("FILESYSTEM")) {
          cardLayout.show(dataPanel, "FILESYSTEM");
        }
        else if (source.equalsIgnoreCase("TABLE")) {
          cardLayout.show(dataPanel, "TABLE");
        }
      }
    });
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
