package com.dbn.object.factory.ui;

import com.dbn.assistant.service.selectai.credential.ui.CredentialEditDialog;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.model.DBAIModelFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBCredentialType;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.FileChoosers.extensionFilter;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.object.type.DBCredentialType.PASSWORD;
import static com.dbn.object.type.DBCredentialType.TOKEN;
import static com.dbn.vector.common.ModelSourceType.MODEL_FILE;
import static com.dbn.vector.common.ModelSourceType.OBJECT_STORAGE;

public class DBAIModelFactoryInputForm extends ObjectFactoryInputForm<DBAIModelFactoryInput> {
    public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = FileChoosers.singleFile().
            withTitle("Select ONNX Model File").
            withDescription("Select a valid ONNX file").
            withFileFilter(extensionFilter("onnx"));

    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;
    private DBNComboBox<DBCredential> credentialComboBox;
    private DBNComboBox<ModelSourceType> sourceComboBox;
    private JTextField nameTextField;
    private TextFieldWithBrowseButton modelFileTextField;
    private JTextField objectUrlTextField;
    private JLabel fileLabel;
    private JLabel objectUrlLabel;
    private JButton credentialAddButton;
    private JLabel credentialLabel;
    private JPanel hyperLinkPanel;

    public DBAIModelFactoryInputForm(DBNComponent parent, DBSchema schema) {
        this(parent, new DBAIModelFactoryInput(schema));
    }

    public DBAIModelFactoryInputForm(DBNComponent parent, DBAIModelFactoryInput input) {
        super(parent, input);
        DBSchema schema = input.getSchema();

        modelFileTextField.addBrowseFolderListener(null, null, getProject(), FILE_CHOOSER_DESCRIPTOR);

        ConnectionHandler connection = input.getConnection();
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

        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());
        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));

        initDocumentationLinkPanel();
        updatePathControls();
        initCredentialFields();
        setListeners();
    }

    private void initDocumentationLinkPanel() {
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Documentation:",
                "ONNX ML Model Import into DB ",
                "https://blogs.oracle.com/machinelearning/use-our-prebuilt-onnx-model-now-available-for-embedding-generation-in-oracle-database-23ai#:~:text=https%3A//adwc4pm.objectstorage.us%2Dashburn%2D1.oci.customer%2Doci.com/p/eLddQappgBJ7jNi6Guz9m9LOtYe2u8LWY19GfgU8flFK4N9YgP4kTlrE9Px3pE12/n/adwc4pm/b/OML%2DResources/o/");
        hyperLinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a name for the new model");
        addTextValidation(modelFileTextField.getTextField(), n -> isNotEmptyOrSpaces(n), "Please select a model file");
        addTextValidation(objectUrlTextField, n -> isNotEmptyOrSpaces(n), "Please provide an object URL");
//    addSelectionValidation(credentialComboBox, "Please select or create a credential");
    }

    private void initCredentialFields() {
        credentialComboBox.set(HIDE_DESCRIPTION, true);
        credentialAddButton.setIcon(Icons.ACTION_ADD);
        credentialAddButton.setText(null);
        credentialComboBox.initialize(() -> loadCredentials());

        ConnectionHandler connection = getConnection();
        List<DBCredentialType> credentialTypes = List.of(TOKEN, PASSWORD);
        credentialAddButton.addActionListener(e -> Dialogs.show(() -> new CredentialEditDialog(connection, null, credentialTypes, Set.of())));

        ObjectChangeEvent.subscribe(this,
                getConnection(),
                DBObjectType.CREDENTIAL,
                () -> credentialComboBox.reloadValues());
    }

    private DBSchema getSchema() {
        return getFactoryInput().getSchema();
    }

    @Override
    protected String getSchemaName() {
        return getSchema().getName();
    }

    @Override
    protected String getObjectName() {
        return nameTextField.getText().trim();
    }

    private List<DBCredential> loadCredentials() {
        List<DBCredential> credentials = getSchema().getCredentials();
        return filter(credentials, c -> c.getType().isOneOf(TOKEN, PASSWORD));
    }

    private void setListeners() {
        ActionListener actionListener = (e) -> updatePathControls();
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
    public void applyFormChanges() {
        factoryInput.setObjectName(getText(nameTextField));
        factoryInput.setCredential(getCredential());
        factoryInput.setSourceType(getModelSourceType());
        factoryInput.setSourceLocation(getModelSourceLocation());
    }

    private DBObjectRef<DBCredential> getCredential() {
        return DBObjectRef.of(getSelection(credentialComboBox));
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(factoryInput.getObjectName());
        modelFileTextField.setText(factoryInput.getSourceLocation());
    }

    private ModelSourceType getModelSourceType() {
        return getSelection(sourceComboBox);
    }

    private String getModelSourceLocation() {
        ModelSourceType sourceType = getModelSourceType();
        return sourceType == MODEL_FILE ?
                modelFileTextField.getText() :
                objectUrlTextField.getText();
    }

    @Override
    public void focus() {
        nameTextField.requestFocus();
    }
}
