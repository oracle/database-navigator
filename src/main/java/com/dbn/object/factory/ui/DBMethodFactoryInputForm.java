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
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.Borders;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.database.DatabaseFeature;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.model.DBArgumentFactoryInput;
import com.dbn.object.factory.model.DBMethodFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;

public class DBMethodFactoryInputForm extends ObjectFactoryInputForm<DBMethodFactoryInput> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel returnDataTypeEditor;
    private JPanel returnArgumentPanel;
    private JLabel returnArgumentIconLabel;
    private JPanel argumentListComponent;
    private JPanel headerPanel;
    private JLabel nameLabel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;


    private DBArgumentFactoryInputListForm argumentListForm;

    public DBMethodFactoryInputForm(DBNComponent parent, DBSchema schema, DBObjectType methodType) {
        this(parent, new DBMethodFactoryInput(schema, methodType));
    }

    public DBMethodFactoryInputForm(DBNComponent parent, DBMethodFactoryInput input) {
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


        returnArgumentPanel.setVisible(hasReturnArgument());
        returnArgumentPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        returnArgumentIconLabel.setText(null);
        returnArgumentIconLabel.setIcon(Icons.DBO_ARGUMENT_OUT);

        DBObjectType objectType = input.getObjectType();
        nameLabel.setText(
                objectType == DBObjectType.FUNCTION ? "Function name" :
                objectType == DBObjectType.PROCEDURE ? "Procedure name" : "Name");

        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());
        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));

        resetFormChanges();
    }

    @Override
    protected void initValidation() {
        String objectTypeName = getObjectType().getName();
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a " + objectTypeName + " name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid " + objectTypeName + " name");

        if (hasReturnArgument()) {
            addTextValidation(getReturnDataTypeEditor().getTextField(), t -> isNotEmptyOrSpaces(t), "Please enter the return argument data type");
        }
    }

    public DataTypeEditor getReturnDataTypeEditor() {
        return (DataTypeEditor) returnDataTypeEditor;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        factoryInput.setObjectName(getText(nameTextField));
        argumentListForm.applyFormChanges();

        DBArgumentFactoryInput returnArgument = factoryInput.getReturnArgument();
        if (returnArgument != null) {
            String dataType = getReturnDataTypeEditor().getDataTypeRepresentation();
            returnArgument.setDataType(dataType);
        }
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(factoryInput.getObjectName());
        argumentListForm.resetFormChanges();

        DBArgumentFactoryInput returnArgument = factoryInput.getReturnArgument();
        if (returnArgument != null) {
            getReturnDataTypeEditor().setText(returnArgument.getDataType());
        }
    }

    protected String getSchemaName() {
        return getFactoryInput().getSchema().getName();
    }

    @Override
    protected String getObjectName() {
        return getText(nameTextField);
    }

    public boolean hasReturnArgument() {
        return getFactoryInput().getObjectType() == DBObjectType.FUNCTION;
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
