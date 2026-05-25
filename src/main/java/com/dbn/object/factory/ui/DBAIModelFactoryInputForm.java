package com.dbn.object.factory.ui;

import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.FileChoosers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBAIModelSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBAIModelSourceType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.FileChoosers.addFileChooser;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.object.type.DBAIModelSourceType.MODEL_FILE;
import static com.dbn.object.type.DBAIModelSourceType.OBJECT_STORAGE;
import static com.dbn.object.type.DBCredentialType.PASSWORD;
import static com.dbn.object.type.DBCredentialType.TOKEN;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class DBAIModelFactoryInputForm extends DBObjectFactoryInputForm<DBAIModelSpec> {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;

    private DBNComboBox<DBAIModelSourceType> sourceComboBox;
    private JTextField nameTextField;
    private TextFieldWithBrowseButton modelFileTextField;
    private JTextField objectUrlTextField;
    private JLabel modelFileLabel;
    private JLabel objectUrlLabel;
    private JLabel credentialLabel;
    private JLabel credentialSchemaLabel;
    private JPanel hyperLinkPanel;

    private DBObjectSelector<DBSchema> credentialSchemaComboBox;
    private DBObjectSelector<DBCredential> credentialComboBox;

    public DBAIModelFactoryInputForm(DBNComponent parent, DBAIModelSpec input) {
        super(parent, input);

        initHeaderForm();
        initComboBoxes();
        initModelFileBrowser();
        initDocumentationLink();
    }

    private void initModelFileBrowser() {
        addFileChooser(getProject(), modelFileTextField, modelFileChooser());
    }

    private static FileChooserDescriptor modelFileChooser() {
        FileChooserDescriptor descriptor = FileChoosers.singleFile().
                withTitle("Select Model File").
                withDescription("Select an ONNX model file (.onnx)")/*.
                withFileFilter(extensionFilter("onnx"))*/;

        return FileChoosers.withExtensionFilter(descriptor, "onnx");
    }

    private void initHeaderForm() {
        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());
        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        ObjectFactoryManager factoryManager = ObjectFactoryManager.getInstance(project);

        StateAttributes state = factoryManager.getState(getObjectType());
        initPersistence(sourceComboBox, state, "model-source-selection");
    }

    private void initComboBoxes() {
        DBAIModelSpec input = getInput();
        ConnectionHandler connection = input.getConnection();
        DBSchema schema = input.getSchema();

        // model context combo-boxes
        connectionComboBox.setValues(connection);
        connectionComboBox.setSelectedValue(connection);
        connectionComboBox.set(HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(false); // TODO support connection switch

        SchemaId schemaId = schema.getSchemaId();
        schemaComboBox.setValues(schemaId);
        schemaComboBox.setSelectedValue(schemaId);
        schemaComboBox.set(HIDE_DESCRIPTION, true);
        schemaComboBox.setEnabled(false); // TODO support connection switch

        // model source combo-box
        initComboBox(sourceComboBox, DBAIModelSourceType.values());
        setSelection(sourceComboBox, MODEL_FILE);
        onSelectionChange(sourceComboBox, e -> updateFieldAvailability());

        // credential combo-boxes
        credentialSchemaComboBox
                .initialize(this, SCHEMA)
                .withConnectionContext(() -> getConnection())
                .withValueLoader(() -> loadSchemas())
                .withValuePreselector(() -> input.getSchemaName())
                .triggerLoad();

        credentialComboBox
                .initialize(this, CREDENTIAL)
                .withConnectionContext(() -> getConnection())
                .withSchemaContext(() -> getCredentialSchema())
                .withValueLoader(() -> loadCredentials())
                .withObjectFactory("New Credential...")
                .triggerLoad();
        updateFieldAvailability();
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(credentialSchemaComboBox, s -> populateCredentials());
    }

    private void populateCredentials() {
        updateFieldAvailability();
        credentialComboBox.reloadValues();
    }


    private DBSchema getCredentialSchema() {
        return getSelection(credentialSchemaComboBox);
    }

    private void initDocumentationLink() {
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Documentation:",
                "ONNX ML Model Import into DB ",
                "https://blogs.oracle.com/machinelearning/use-our-prebuilt-onnx-model-now-available-for-embedding-generation-in-oracle-database-23ai#:~:text=https%3A//adwc4pm.objectstorage.us%2Dashburn%2D1.oci.customer%2Doci.com/p/eLddQappgBJ7jNi6Guz9m9LOtYe2u8LWY19GfgU8flFK4N9YgP4kTlrE9Px3pE12/n/adwc4pm/b/OML%2DResources/o/");
        hyperLinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(
                () -> isLocalFileSource(), array(
                        modelFileLabel,
                        modelFileTextField));

        fieldAdapter.initFieldsVisibility(
                () -> isObjectStorageSource(), array(
                        objectUrlLabel,
                        objectUrlTextField,
                        credentialSchemaLabel,
                        credentialSchemaComboBox,
                        credentialLabel,
                        credentialComboBox));

        fieldAdapter.initFieldsAvailability(
                () -> isValid(getCredentialSchema()), array(credentialComboBox));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a name for the new model");
        addTextValidation(modelFileTextField.getTextField(), n -> isNotEmptyOrSpaces(n), "Please select a model file");
        addTextValidation(objectUrlTextField, n -> isNotEmptyOrSpaces(n), "Please provide an object URL");
//    addSelectionValidation(credentialComboBox, "Please select or create a credential");
    }

    private DBSchema getSchema() {
        return getInput().getSchema();
    }

    @Override
    protected String getSchemaName() {
        return getSchema().getName();
    }

    @Override
    protected String getObjectName() {
        return nameTextField.getText().trim();
    }

    private List<DBSchema> loadSchemas() {
        DBObjectBundle objectBundle = getConnection().getObjectBundle();
        return objectBundle.getSchemas();
    }

    private List<DBCredential> loadCredentials() {
        DBSchema credentialSchema = getCredentialSchema();
        if (credentialSchema == null) return emptyList();

        List<DBCredential> credentials = credentialSchema.getCredentials();
        return filter(credentials, c -> c.getType().isOneOf(TOKEN, PASSWORD));
    }

    private boolean isLocalFileSource() {
        return sourceComboBox.getSelectedValue() == MODEL_FILE;
    }

    private boolean isObjectStorageSource() {
        return sourceComboBox.getSelectedValue() == OBJECT_STORAGE;
    }

    @Override
    public @NotNull JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() {
        input.setObjectName(getText(nameTextField));
        input.setCredential(getCredential());
        input.setSourceType(getModelSourceType());
        input.setSourceLocation(getModelSourceLocation());
    }

    private DBObjectRef<DBCredential> getCredential() {
        return DBObjectRef.of(getSelection(credentialComboBox));
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(input.getObjectName());
        modelFileTextField.setText(input.getSourceLocation());
    }

    private DBAIModelSourceType getModelSourceType() {
        return getSelection(sourceComboBox);
    }

    private String getModelSourceLocation() {
        DBAIModelSourceType sourceType = getModelSourceType();
        return sourceType == MODEL_FILE ?
                modelFileTextField.getText() :
                objectUrlTextField.getText();
    }

    @Override
    public void focus() {
        nameTextField.requestFocus();
    }
}
