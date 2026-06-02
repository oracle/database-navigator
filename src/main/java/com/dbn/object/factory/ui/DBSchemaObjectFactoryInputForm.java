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

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;

public abstract class DBSchemaObjectFactoryInputForm extends DBObjectFactoryInputForm {

    public DBSchemaObjectFactoryInputForm(@NotNull DBNComponent parent, DBObjectSpec input) {
        super(parent, input);
    }

    protected void initContextComponents(){
        ConnectionHandler connection = getConnection();
        DBNComboBox<ConnectionHandler> connectionComboBox = getConnectionComboBox();
        connectionComboBox.setValues(connection);
        connectionComboBox.setSelectedValue(connection);
        connectionComboBox.set(HIDE_DESCRIPTION, true);
        connectionComboBox.setEnabled(false); // TODO support connection switch

        SchemaId schemaId = input.getSchemaId();
        DBNComboBox<SchemaId> schemaComboBox = getSchemaComboBox();
        schemaComboBox.setValues(schemaId);
        schemaComboBox.setSelectedValue(schemaId);
        schemaComboBox.set(HIDE_DESCRIPTION, true);
        schemaComboBox.setEnabled(false); // TODO support connection switch
    }

    protected void initHeaderForm() {
        DBNHeaderForm headerForm = createHeaderForm();
        getHeaderPanel().add(headerForm.getComponent());
        onTextChange(getNameTextField(), e -> headerForm.setTitle(buildHeaderTitle()));
    }

    @Override
    public void resetFormChanges() {
        setText(getNameTextField(), input.getObjectName());
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        input.setObjectName(getObjectName());
    }

    @Override
    protected String getObjectName() {
        return getText(getNameTextField());
    }

    @Override
    protected String getSchemaName() {
        return input.getSchemaName();
    }

    protected final DatabaseIdentifierCase getDefaultIdentifierCase() {
        return getConnection().getCompatibility().getIdentifierCase();
    }

    protected abstract DatabaseIdentifierCase getSelectedIdentifierCase();

    protected final TextContent getPreserveCaseInfoText() {
        String databaseTypeName = getConnection().getDatabaseType().getName();
        String infoText = String.format(
                "<strong>Preserve identifier case</strong><br><br>" +
                "Preserve identifier names exactly as typed, instead of applying the %s default identifier casing (%s) before creation.<br><br>" +
                "Note: Identifiers that require quoting, such as reserved words or names containing non-alphanumeric characters, are preserved automatically.",
                databaseTypeName,
                getDefaultIdentifierCase());
        return TextContent.tooltip(infoText, "width:200px");
    }

    @Override
    public void focus() {
        getNameTextField().requestFocus();
    }

    protected abstract DBNComboBox<ConnectionHandler> getConnectionComboBox();
    protected abstract DBNComboBox<SchemaId> getSchemaComboBox();
    protected abstract JPanel getHeaderPanel();
    protected abstract JTextField getNameTextField();
}
