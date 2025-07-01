package com.dbn.object.factory.ui;

import com.dbn.assistant.credential.remote.ui.CredentialEditDialog;
import com.dbn.common.color.Colors;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.FileChoosers;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.factory.ModelFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelPathType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Commons.nvln;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Strings.*;

public class ModelFactoryInputForm extends ObjectFactoryInputForm<ModelFactoryInput> {
  public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.singleFile().
          withTitle("Select ONNX Model File").
          withDescription("Select a valid ONNX file").
          withFileFilter(virtualFile -> Objects.equals(virtualFile.getExtension(), "onnx"));

  private JPanel mainPanel;
  private JPanel headerPanel;
  private DBNComboBox<ConnectionHandler> connectionComboBox;
  private DBNComboBox<SchemaId> schemaComboBox;
  private JRadioButton localFileRadioButton;
  private JRadioButton objectStorageRadioButton;
  private JTextField modelNameTextField;
  private TextFieldWithBrowseButton onnxModel;
  private JTextField objectUrl;
  private JLabel fileLabel;
  private JLabel objectUrlLabel;
  private JComboBox credentialDBNComboBox;
  private JButton addCredentialButton;
  private JLabel credentialLabel;

  private final DBObjectRef<DBSchema> schema;

  public ModelFactoryInputForm(DBNComponent parent, DBSchema schema,DBObjectType objectType, int index) {


    super(parent,schema.getConnection(),DBObjectType.AI_MODEL,index);
    this.schema = DBObjectRef.of(schema);
    onnxModel.addBrowseFolderListener(
            getProject(),
            FILE_CHOOSER_DESCRIPTOR);

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

    DBNHeaderForm headerForm = createHeaderForm(schema,objectType);
    ButtonGroup pathGroup = new ButtonGroup();
    pathGroup.add(localFileRadioButton);
    pathGroup.add(objectStorageRadioButton);
    onTextChange(modelNameTextField, e -> headerForm.setTitle(schema.getName() + "." + toUpperCase(getObjectName()))); // TODO support quoted names
    whenFirstShown(mainPanel, () -> populateCredentials());

    initCredentialAddButton();
    setListeners();
  }


  private void initCredentialAddButton() {
    addCredentialButton.setIcon(Icons.ACTION_ADD);
    addCredentialButton.setText(null);

    ConnectionHandler connection = getConnection();
    addCredentialButton.addActionListener(e -> Dialogs.show(() -> new CredentialEditDialog(connection, null, Set.of())));

    Project project = connection.getProject();
    ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType, operation) -> {
      if (connectionId != connection.getConnectionId()) return;
      if (objectType != DBObjectType.CREDENTIAL) return;
      populateCredentials();
    });
  }

  private void populateCredentials() {
    ConnectionHandler connection = getConnection();
    Project project = connection.getProject();

    Background.run(() -> {

      java.util.List<DBCredential> credentials = schema.getSchema().getCredentials();
      List<String> credentialNames = convert(credentials, c -> c.getName());


      credentialDBNComboBox.removeAllItems();
      credentialNames.forEach(c -> credentialDBNComboBox.addItem(c));
    });
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
    headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    return headerForm;
  }


  private void setListeners() {
    ActionListener actionListener = (e)->updatePathControls();

    localFileRadioButton.addActionListener(actionListener);

    objectStorageRadioButton.addActionListener(actionListener);
  }

  private void updatePathControls() {
    boolean local = localFileRadioButton.isSelected();
    fileLabel.setVisible(local);
    onnxModel.setVisible(local);
    objectUrlLabel.setVisible(!local);
    objectUrl.setVisible(!local);
    credentialDBNComboBox.setVisible(!local);
    addCredentialButton.setVisible(!local);
    credentialLabel.setVisible(!local);
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
    return new ModelFactoryInput(
            schema.getSchema(),
            modelNameTextField.getText(),
            localFileRadioButton.isSelected() ? ModelPathType.LOCAL:ModelPathType.OBJECT_STORAGE,
            localFileRadioButton.isSelected() ? onnxModel.getText() : objectUrl.getText(),
            (String) credentialDBNComboBox.getSelectedItem()
    ) ;
  }

  @Override
  public void focus() {
    objectStorageRadioButton.requestFocus();
  }
}
