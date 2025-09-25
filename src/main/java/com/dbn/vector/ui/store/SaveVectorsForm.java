package com.dbn.vector.ui.store;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.Disposable;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class SaveVectorsForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<DestinationType> destinationComboBox;
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

    DestinationType destinationType = getDestinationType();
    JComponent initialPanel = destinationType == DestinationType.NEW_TABLE
        ? createForm.getMainComponent()
        : existingForm.getMainComponent();
    dataPanel.add(initialPanel);
  }

  private void initToggleButton() {
    ComboBoxes.initComboBox(destinationComboBox, DestinationType.values());
    setSelection(destinationComboBox, DestinationType.NEW_TABLE);

    destinationComboBox.addActionListener(e -> {
      dataPanel.removeAll();
      if (getDestinationType() == DestinationType.NEW_TABLE) {
        dataPanel.add(createForm.getMainComponent());

      } else {
        dataPanel.add(existingForm.getMainComponent());
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
    return getDestinationType() == DestinationType.NEW_TABLE
        ? createForm.toStoreConfig()
        : existingForm.toStoreConfig();
  }
  public DestinationType getDestinationType() {
    return ComboBoxes.getSelection(destinationComboBox);
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

  @Getter
  public enum DestinationType implements Presentable {
    EXISTING_TABLE("Existing table"),
    NEW_TABLE("New table");

    private final String name;
    DestinationType(String name) { this.name = name; }
  }
}
