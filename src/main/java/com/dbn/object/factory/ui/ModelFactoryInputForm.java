package com.dbn.object.factory.ui;

import com.dbn.assistant.service.selectai.credential.ui.CredentialEditDialog;
import com.dbn.common.color.Colors;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.factory.ModelFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.toUpperCase;
import static com.dbn.object.type.DBCredentialType.getVectorAITypes;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.vector.common.ModelSourceType.MODEL_FILE;
import static com.dbn.vector.common.ModelSourceType.OBJECT_STORAGE;

public class ModelFactoryInputForm extends ObjectFactoryInputForm<ModelFactoryInput> {
  public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.singleFile().
          withTitle("Select ONNX Model File").
          withDescription("Select a valid ONNX file").
          withExtensionFilter("onnx");

  private JPanel mainPanel;
  private JPanel headerPanel;
  private DBNComboBox<ConnectionHandler> connectionComboBox;
  private DBNComboBox<SchemaId> schemaComboBox;
  private DBNComboBox<DBCredential> credentialComboBox;
  private DBNComboBox<ModelSourceType> sourceComboBox;
  private JTextField modelNameTextField;
  private TextFieldWithBrowseButton modelFileTextField;
  private JTextField objectUrlTextField;
  private JLabel fileLabel;
  private JLabel objectUrlLabel;
  private JButton credentialAddButton;
  private JLabel credentialLabel;

  private final DBObjectRef<DBSchema> schema;

  public ModelFactoryInputForm(DBNComponent parent, DBSchema schema,DBObjectType objectType, int index) {
    super(parent,schema.getConnection(),DBObjectType.AI_MODEL,index);

    this.schema = DBObjectRef.of(schema);
    modelFileTextField.addBrowseFolderListener(getProject(), FILE_CHOOSER_DESCRIPTOR);

    ConnectionHandler connection = getConnection();
    connectionComboBox.setValues(connection);
    connectionComboBox.setSelectedValue(connection);
    connectionComboBox.set(HIDE_DESCRIPTION, true);
    connectionComboBox.setEnabled(false); // TODO support connection switch

    SchemaId schemaId = schema.getSchemaId();
    schemaComboBox.setValues(schemaId);
    schemaComboBox.setSelectedValue(schemaId);
    schemaComboBox.set(HIDE_DESCRIPTION, true);
    schemaComboBox.setEnabled(false); // TODO support connection switch

    initComboBox(sourceComboBox, ModelSourceType.values());
    setSelection(sourceComboBox, MODEL_FILE);

    DBNHeaderForm headerForm = createHeaderForm(schema,objectType);
    onTextChange(modelNameTextField, e -> headerForm.setTitle(schema.getName() + "." + toUpperCase(getObjectName()))); // TODO support quoted names

    updatePathControls();
    initCredentialFields();
    setListeners();
  }

  @Override
  protected void initValidation() {
    addTextValidation(modelNameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a name for the new model");
    addTextValidation(modelFileTextField.getTextField(), n -> isNotEmptyOrSpaces(n), "Please select a model file");
    addTextValidation(objectUrlTextField, n -> isNotEmptyOrSpaces(n), "Please provide an object URL");
    addSelectionValidation(credentialComboBox, "Please select or create a credential");
  }

  private void initCredentialFields() {
    credentialComboBox.set(HIDE_DESCRIPTION, true);
    credentialComboBox.init(() -> loadCredentials(), null);
    credentialAddButton.setIcon(Icons.ACTION_ADD);
    credentialAddButton.setText(null);

    ConnectionHandler connection = getConnection();
    credentialAddButton.addActionListener(e -> Dialogs.show(() -> new CredentialEditDialog(connection, null, getVectorAITypes(), Set.of())));

    Project project = connection.getProject();
    ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, e -> {
      if (!e.matches(connection)) return;
      if (!e.matches(CREDENTIAL)) return;
      credentialComboBox.reloadValues();
    });
  }

  private DBSchema getSchema() {
    return DBObjectRef.ensure(schema);
  }

  private List<DBCredential> loadCredentials() {
    List<DBCredential> credentials = getSchema().getCredentials();
    return filter(credentials, c -> getVectorAITypes().contains(c.getType()));
  }

  private DBNHeaderForm createHeaderForm(DBSchema schema,DBObjectType objectType) {
    String headerTitle = schema.getName() + ".[unnamed]";
    Icon headerIcon = objectType.getIcon();
    Color headerBackground = Colors.getPanelBackground();
    if (getEnvironmentSettings(schema.getProject()).getVisibilitySettings().getDialogHeaders().value()) {
      headerBackground = schema.getEnvironmentType().getColor();
    }
    DBNHeaderForm headerForm = new DBNHeaderForm(
            this, headerTitle,
            headerIcon,
            headerBackground
    );
    headerPanel.add(headerForm.getComponent());
    return headerForm;
  }


  private void setListeners() {
    ActionListener actionListener = (e)->updatePathControls();
    sourceComboBox.addActionListener(actionListener);
  }

  private void updatePathControls() {
    ModelSourceType source = getModelSourceType();
    boolean local = source == MODEL_FILE;
    boolean storage = source == OBJECT_STORAGE;

    fileLabel.setVisible(local);
    modelFileTextField.setVisible(local);
    objectUrlLabel.setVisible(storage);
    objectUrlTextField.setVisible(storage);
    credentialComboBox.setVisible(storage);
    credentialAddButton.setVisible(storage);
    credentialLabel.setVisible(storage);
  }


  @Override
  public @NotNull JPanel getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getObjectName() {
    return modelNameTextField.getText().trim();
  }

  @Override
  public ModelFactoryInput createFactoryInput(ObjectFactoryInput parent) {
    ModelSourceType sourceType = getModelSourceType();
    String sourceLocation = sourceType == MODEL_FILE ?
            modelFileTextField.getText() :
            objectUrlTextField.getText();
    DBCredential credential = getSelection(credentialComboBox);

    return new ModelFactoryInput(
            schema.getSchema(),
            modelNameTextField.getText(),
            sourceType,
            sourceLocation,
            credential) ;
  }

  private ModelSourceType getModelSourceType() {
    return getSelection(sourceComboBox);
  }

  @Override
  public void restoreUserInput(@Nullable ModelFactoryInput input) {
    if (input == null) return;

    modelNameTextField.setText(input.getModelName());
    modelFileTextField.setText(input.getSourceLocation());
  }

  @Override
  public void focus() {
    modelNameTextField.requestFocus();
  }
}
