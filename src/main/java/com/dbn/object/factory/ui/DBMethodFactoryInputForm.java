/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.object.factory.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ObjectFactoryManager;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;

public class DBMethodFactoryInputForm extends DBSchemaObjectFactoryInputForm {
    private JPanel mainPanel;
    private @Getter JPanel headerPanel;
    private @Getter JTextField nameTextField;
    private JPanel returnDataTypeEditor;
    private JPanel returnArgumentPanel;
    private JLabel nameLabel;
    private JLabel returnArgumentIconLabel;
    private JPanel argumentListComponent;
    private @Getter DBNComboBox<ConnectionHandler> connectionComboBox;
    private @Getter DBNComboBox<SchemaId> schemaComboBox;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;


    private DBArgumentFactoryInputListForm argumentListForm;

    public DBMethodFactoryInputForm(DBNComponent parent, DBObjectSpec input) {
        super(parent, input);
        DBSchema schema = input.getSchema();

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

        initPreserveCaseFields();

        returnArgumentPanel.setVisible(hasReturnArgument());
        returnArgumentPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        returnArgumentIconLabel.setText(null);
        returnArgumentIconLabel.setIcon(Icons.DBO_ARGUMENT_OUT);

        DBObjectType objectType = input.getObjectType();
        nameLabel.setText(
                objectType == DBObjectType.FUNCTION ? txt("app.object.label.FunctionName") :
                objectType == DBObjectType.PROCEDURE ? txt("app.object.label.ProcedureName") :
                txt("app.object.label.GenericName"));

        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());
        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));

        resetFormChanges();
    }

    private void initPreserveCaseFields() {
        preserveCaseInfoLabel.setContent(getPreserveCaseInfoText());
    }

    @Override
    protected void initStatePersistence() {
        Project project = ensureProject();
        ObjectFactoryManager factoryManager = ObjectFactoryManager.getInstance(project);

        StateAttributes state = factoryManager.getState(getObjectType());
        initPersistence(preserveCaseCheckBox, state, "preserve-identifier-case");
    }

    @Override
    protected void initValidation() {
        String objectTypeName = getObjectType().getDisplayName();
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), txt("msg.objects.error.ObjectNameRequired", objectTypeName));
        addTextValidation(nameTextField, n -> isWord(n), txt("msg.objects.error.ValidObjectNameRequired", objectTypeName));

        if (hasReturnArgument()) {
            addTextValidation(getReturnDataTypeEditor().getTextField(), t -> isNotEmptyOrSpaces(t), txt("msg.objects.error.ReturnArgumentDataTypeRequired"));
        }
    }

    public DataTypeEditor getReturnDataTypeEditor() {
        return (DataTypeEditor) returnDataTypeEditor;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        input.setObjectName(getText(nameTextField));
        input.setIdentifierCase(getSelectedIdentifierCase());
        argumentListForm.applyFormChanges();

        DBObjectSpec returnArgument = RETURN_ARGUMENT.of(input);
        if (returnArgument != null) {
            String dataType = getReturnDataTypeEditor().getDataTypeRepresentation();
            returnArgument.setAttributeValue(DATA_TYPE, dataType);
        }
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(input.getObjectName());
        argumentListForm.resetFormChanges();

        DBObjectSpec returnArgument = RETURN_ARGUMENT.of(input);
        if (returnArgument != null) {
            String dataType = DATA_TYPE.of(returnArgument);
            getReturnDataTypeEditor().setText(dataType);
        }
    }

    @Override
    protected String getSchemaName() {
        return getInput().getSchema().getName();
    }

    @Override
    protected String getObjectName() {
        return getText(nameTextField);
    }

    @Override
    protected DatabaseIdentifierCase getSelectedIdentifierCase() {
        return preserveCaseCheckBox.isSelected() ?
                DatabaseIdentifierCase.PRESERVE :
                getDefaultIdentifierCase();
    }

    public boolean hasReturnArgument() {
        return getInput().getObjectType() == DBObjectType.FUNCTION;
    }

    public boolean enforceInArguments() {
        ConnectionHandler connection = getConnection();
        return hasReturnArgument() && !DatabaseFeature.FUNCTION_OUT_ARGUMENTS.isSupported(connection);
    }

    private void createUIComponents() {
        argumentListForm = new DBArgumentFactoryInputListForm(this);
        argumentListComponent = (JPanel) argumentListForm.getComponent();
        returnDataTypeEditor = new DataTypeEditor(getConnection());
    }

    @Override
    public void focus() {
        nameTextField.requestFocus();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
