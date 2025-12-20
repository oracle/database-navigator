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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.factory.model.DBTableSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;

public class DBTableFactoryInputForm extends DBObjectFactoryInputForm<DBTableSpec> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel columnListPanel;
    private JPanel headerPanel;
    private JLabel nameLabel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;

    private DBColumnFactoryInputListForm columnListForm;

    public DBTableFactoryInputForm(DBNComponent parent, DBTableSpec input) {
        super(parent, input);

        ConnectionHandler connection = getConnection();
        connectionComboBox.setValues(connection);
        connectionComboBox.setSelectedValue(connection);
        connectionComboBox.set(HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(false); // TODO support connection switch

        SchemaId schemaId = input.getSchemaId();
        schemaComboBox.setValues(schemaId);
        schemaComboBox.setSelectedValue(schemaId);
        schemaComboBox.set(HIDE_DESCRIPTION, true);
        schemaComboBox.setEnabled(false); // TODO support connection switch

        nameLabel.setText("Table name");

        DBNHeaderForm headerForm = createHeaderForm();
        headerPanel.add(headerForm.getComponent());
        onTextChange(nameTextField, e -> headerForm.setTitle(buildHeaderTitle()));

        resetFormChanges();
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a table name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid table name");
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        input.setObjectName(getText(nameTextField));
        columnListForm.applyFormChanges();
    }

    @Override
    public void resetFormChanges() {
        setText(nameTextField, input.getObjectName());
        columnListForm.resetFormChanges();
    }

    protected String getSchemaName() {
        return getInput().getSchema().getName();
    }

    @Override
    protected String getObjectName() {
        return getText(nameTextField);
    }

    private void createUIComponents() {
        columnListForm = new DBColumnFactoryInputListForm(this);
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
