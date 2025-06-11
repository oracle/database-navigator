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

import com.dbn.common.color.Colors;
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
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.MethodFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.common.util.Strings.toUpperCase;

public abstract class MethodFactoryInputForm extends ObjectFactoryInputForm<MethodFactoryInput> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel returnArgumentPanel;
    private JLabel returnArgumentIconLabel;
    private JPanel headerPanel;
    private JLabel nameLabel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;

    private DataTypeEditor returnDataTypeEditor;

    private ArgumentFactoryInputListForm argumentListPanel;
    private final DBObjectRef<DBSchema> schema;

    public MethodFactoryInputForm(DBNComponent parent, DBSchema schema, DBObjectType objectType, int index) {
        super(parent, schema.getConnection(), objectType, index);
        this.schema = DBObjectRef.of(schema);

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
        argumentListPanel.createObjectPanel(null);
        //argumentListPanel.createObjectPanel();
        //argumentListPanel.createObjectPanel();

        returnArgumentIconLabel.setText(null);
        returnArgumentIconLabel.setIcon(Icons.DBO_ARGUMENT_OUT);

        nameLabel.setText(
                objectType == DBObjectType.FUNCTION ? "Function name" :
                objectType == DBObjectType.PROCEDURE ? "Procedure name" : "Name");

        DBNHeaderForm headerForm = createHeaderForm(schema, objectType);
        onTextChange(nameTextField, e -> headerForm.setTitle(getSchema().getName() + "." + toUpperCase(getObjectName()))); // TODO support quoted names
    }

    @Override
    protected void initValidation() {
        String objectTypeName = getObjectType().getName();
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a " + objectTypeName + " name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid " + objectTypeName + " name");

        if (hasReturnArgument()) {
            addTextValidation(returnDataTypeEditor.getTextField(), t -> isNotEmptyOrSpaces(t), "Please enter the return argument data type");
        }
    }


    private DBNHeaderForm createHeaderForm(DBSchema schema, DBObjectType objectType) {
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

    @Override
    public MethodFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        MethodFactoryInput input = new MethodFactoryInput(getSchema(), nameTextField.getText(), getObjectType());
        input.setArguments(argumentListPanel.createFactoryInputs(input));

        if (hasReturnArgument()) {

            ArgumentFactoryInput returnArgument = new ArgumentFactoryInput(
                    input, 0, "return",
                    returnDataTypeEditor.getDataTypeRepresentation(),
                    false, true);

            input.setReturnArgument(returnArgument);
        }
        return input;
    }

    @Override
    public void restoreUserInput(MethodFactoryInput input) {
        if (input == null) return;
        argumentListPanel.removeObjectPanel(0); // remove default first argument panel

        nameTextField.setText(input.getObjectName());

        List<ArgumentFactoryInput> argumentInputs = input.getArguments();
        for (ArgumentFactoryInput argumentInput : argumentInputs) {
            ObjectFactoryInputForm<ArgumentFactoryInput> argumentInputForm = argumentListPanel.createObjectPanel(null);
            argumentInputForm.restoreUserInput(argumentInput);
        }

        if (hasReturnArgument()) {
            ArgumentFactoryInput returnArgument = input.getReturnArgument();
            if (returnArgument != null) {
                returnDataTypeEditor.setText(returnArgument.getDataType());
            }
        }
    }

    DBSchema getSchema() {
        return DBObjectRef.get(schema);
    }

    @Override
    public String getObjectName() {
        return nameTextField.getText().trim();
    }

    public abstract boolean hasReturnArgument();

    private void createUIComponents() {
        ConnectionHandler connection = getConnection();
        boolean enforceInArguments = hasReturnArgument() && !DatabaseFeature.FUNCTION_OUT_ARGUMENTS.isSupported(connection);
        argumentListPanel = new ArgumentFactoryInputListForm(this, connection, enforceInArguments);
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
