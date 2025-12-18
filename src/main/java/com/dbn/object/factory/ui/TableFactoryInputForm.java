/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ColumnFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.TableFactoryInput;
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
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.common.util.Strings.toUpperCase;

public class TableFactoryInputForm extends ObjectFactoryInputForm<TableFactoryInput> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel columnListPanel;
    private JPanel headerPanel;
    private JLabel nameLabel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;


    private ColumnFactoryInputListForm columnListForm;
    private final DBObjectRef<DBSchema> schema;

    public TableFactoryInputForm(DBNComponent parent, DBSchema schema, DBObjectType objectType, int index) {
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


        columnListForm.createObjectPanel(null);
        //argumentListPanel.createObjectPanel();
        //argumentListPanel.createObjectPanel();

        nameLabel.setText("Table name");

        DBNHeaderForm headerForm = createHeaderForm(schema, objectType);
        onTextChange(nameTextField, e -> headerForm.setTitle(getSchema().getName() + "." + toUpperCase(getObjectName()))); // TODO support quoted names
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a table name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid table name");
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
    public TableFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        TableFactoryInput input = new TableFactoryInput(getSchema(), getText(nameTextField));
        input.setColumns(columnListForm.createFactoryInputs(input));
        return input;
    }

    @Override
    public void restoreUserInput(TableFactoryInput input) {
        if (input == null) return;
        columnListForm.removeObjectPanel(0); // remove default first column panel

        nameTextField.setText(input.getObjectName());

        List<ColumnFactoryInput> columnInputs = input.getColumns();
        for (ColumnFactoryInput columnInput : columnInputs) {
            ObjectFactoryInputForm<ColumnFactoryInput> argumentInputForm = columnListForm.createObjectPanel(null);
            argumentInputForm.restoreUserInput(columnInput);
        }
    }

    DBSchema getSchema() {
        return DBObjectRef.get(schema);
    }

    @Override
    public String getObjectName() {
        return getText(nameTextField);
    }

    private void createUIComponents() {
        ConnectionHandler connection = getConnection();
        columnListForm = new ColumnFactoryInputListForm(this, connection);
        columnListPanel = (JPanel) columnListForm.getComponent();
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
