package com.dbn.vector.ui.store;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.store.DestinationType;
import com.dbn.vector.model.store.StoreConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class SaveVectorsForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<DestinationType> destinationComboBox;
  private JLabel destinationLabel;
  // Sub-forms (defined as inner classes below)
  private CreateVectorDestinationForm createForm;
  private ExistingTableDestinationForm existingForm;

  public SaveVectorsForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    initDestinationPanel();
    initComboBoxes();
  }

  private void initDestinationPanel() {
    ConnectionHandler connection = getConnection();
    createForm = new CreateVectorDestinationForm(this, connection);
    existingForm = new ExistingTableDestinationForm(this,connection);

    DestinationType destinationType = getDestinationType();
    JComponent initialPanel = destinationType == DestinationType.NEW_TABLE
        ? createForm.getComponent()
        : existingForm.getComponent();
    dataPanel.add(initialPanel);
  }

  private void initComboBoxes() {
    initComboBox(destinationComboBox, DestinationType.values());
    setSelection(destinationComboBox, DestinationType.NEW_TABLE);
    onSelectionChange(destinationComboBox, t -> updateDestinationPanel());
  }

  private void updateDestinationPanel() {
    dataPanel.removeAll();
    if (getDestinationType() == DestinationType.NEW_TABLE) {
      dataPanel.add(createForm.getComponent());

    } else {
      dataPanel.add(existingForm.getComponent());
    }
    dataPanel.revalidate();
    dataPanel.repaint();
    validateFormFields();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public void resetFormChanges() {
    StoreConfig config = getConfig();
    setSelection(destinationComboBox, config.getDestinationType());
    createForm.resetFormChanges();
    existingForm.resetFormChanges();
    updateDestinationPanel();
  }

  @Override
  public void applyFormChanges() {
    StoreConfig config = getConfig();
    DestinationType destinationType = getDestinationType();
    config.setDestinationType(destinationType);
    if (destinationType == DestinationType.NEW_TABLE) {
      createForm.applyFormChanges();
    } else if (destinationType == DestinationType.EXISTING_TABLE) {
      existingForm.applyFormChanges();
    }
  }

  public StoreConfig getConfig() {
    return getEmbeddingRequest().getStoreConfig();
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

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(destinationLabel, destinationComboBox);
    alignerData.registerForms(createForm, existingForm);
  }
}
