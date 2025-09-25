package com.dbn.vector.ui.store;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SaveVectorsForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  JToggleButton toggleButton1;
  // Sub-forms (defined as inner classes below)
  private CreateVectorDestinationForm createForm;
  private ExistingTableDestinationForm existingForm;
  private ConnectionHandler connection;

  public SaveVectorsForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent);
    this.connection = connection;
    initToggleButton();
    initDataPanel();
  }

  private void initDataPanel() {
    createForm = new CreateVectorDestinationForm(this);
    existingForm = new ExistingTableDestinationForm(this,connection);
    dataPanel.setLayout(new BorderLayout());
    JComponent initialPanel = toggleButton1.isSelected()
        ? createForm.getMainComponent()
        : existingForm.getMainComponent();
    dataPanel.add(initialPanel, BorderLayout.CENTER);
  }

  private void initToggleButton() {
    toggleButton1.setText(toggleButton1.isSelected() ? "Create new Table? Yes" : "Create new Table? No");
    toggleButton1.addActionListener(e -> {
      dataPanel.removeAll();
      if (toggleButton1.isSelected()) {
        dataPanel.add(createForm.getMainComponent(), BorderLayout.CENTER);
        toggleButton1.setText("Create new Table? Yes");
      } else {
        dataPanel.add(existingForm.getMainComponent(), BorderLayout.CENTER);
        toggleButton1.setText("Create new Table? No");
      }
      dataPanel.revalidate();
      dataPanel.repaint();
    });
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public StoreConfig getStoreConfig() {
    return toggleButton1.isSelected()
        ? createForm.toStoreConfig()
        : existingForm.toStoreConfig();
  }
  @Override
  public String getCollapsedTitle() {
    return "Embedding Destination";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Embedding Destination";
  }
}
