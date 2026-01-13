package com.dbn.vector.ui.destination;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingDestinationType;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.util.Strings.isNotEmpty;

public class EmbeddingDestinationConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<EmbeddingDestinationType> destinationComboBox;
  private JLabel destinationLabel;
  // Sub-forms (defined as inner classes below)
  private EmbeddingDestinationNewTableForm createForm;
  private EmbeddingDestinationTableForm existingForm;

  public EmbeddingDestinationConfigForm(@NotNull VectorToolboxFormBase parent) {
    super(parent);
    initDestinationPanel();
    initComboBoxes();
  }

  private void initDestinationPanel() {
    createForm = new EmbeddingDestinationNewTableForm(this);
    existingForm = new EmbeddingDestinationTableForm(this);

    EmbeddingDestinationType destinationType = getDestinationType();
    JComponent initialPanel = destinationType == EmbeddingDestinationType.NEW_TABLE
        ? createForm.getComponent()
        : existingForm.getComponent();
    dataPanel.add(initialPanel);
  }

  private void initComboBoxes() {
    initComboBox(destinationComboBox, EmbeddingDestinationType.values());
    setSelection(destinationComboBox, EmbeddingDestinationType.NEW_TABLE);
    onSelectionChange(destinationComboBox, t -> updateDestinationPanel());
  }

  private void updateDestinationPanel() {
    dataPanel.removeAll();
    if (getDestinationType() == EmbeddingDestinationType.NEW_TABLE) {
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
    EmbeddingDestinationConfig config = getConfig();
    setSelection(destinationComboBox, config.getDestinationType());
    createForm.resetFormChanges();
    existingForm.resetFormChanges();
    updateDestinationPanel();
  }

  @Override
  public void applyFormChanges() {
    EmbeddingDestinationConfig config = getConfig();
    EmbeddingDestinationType destinationType = getDestinationType();
    config.setDestinationType(destinationType);
    if (destinationType == EmbeddingDestinationType.NEW_TABLE) {
      createForm.applyFormChanges();
    } else if (destinationType == EmbeddingDestinationType.EXISTING_TABLE) {
      existingForm.applyFormChanges();
    }
  }

  public EmbeddingDestinationConfig getConfig() {
    return getEmbeddingRequest().getDestinationConfig();
  }

  public EmbeddingDestinationType getDestinationType() {
    return ComboBoxes.getSelection(destinationComboBox);
  }

  @Override
  public String getFormTitle() {
    return "Embedding Destination";
  }

  @Override
  public String getFormTitleDetail() {
    EmbeddingDestinationType destinationType = getDestinationType();
    String destinationTypeName = destinationType == null ? null : destinationType.getName();
    if (destinationType == EmbeddingDestinationType.NEW_TABLE) {
      DBSchema schema = createForm.getSelectedSchema();
      String tableName = createForm.getTableName();

      if (schema != null && isNotEmpty(tableName)) {
        return destinationTypeName + " - " + schema.getName() + "." + tableName;
      }
    }

    if (destinationType == EmbeddingDestinationType.EXISTING_TABLE) {
      DBSchema schema = existingForm.getSelectedSchema();
      DBTable table = existingForm.getSelectedTable();

      if (schema != null && table != null) {
        return destinationTypeName + " - " + schema.getName() + "." + table.getName();
      }
    }

    return destinationTypeName;
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(destinationLabel, destinationComboBox);
    alignerData.registerForms(createForm, existingForm);
  }
}
