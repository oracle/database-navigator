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
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.object.factory.model.DBTableSpec;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;

@Getter
public class DBTableFactoryInputForm extends DBSchemaObjectFactoryInputForm<DBTableSpec> {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel columnListPanel;
    private JPanel headerPanel;
    private DBNComboBox<ConnectionHandler> connectionComboBox;
    private DBNComboBox<SchemaId> schemaComboBox;

    private DBColumnFactoryInputListForm columnListForm;

    public DBTableFactoryInputForm(DBNComponent parent, DBTableSpec input) {
        super(parent, input);

        initContextComponents();
        initHeaderForm();

        resetFormChanges();
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a table name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid table name");
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        super.applyFormChanges();
        columnListForm.applyFormChanges();
    }

    @Override
    public void resetFormChanges() {
        super.resetFormChanges();
        columnListForm.resetFormChanges();
    }

    private void createUIComponents() {
        columnListForm = new DBColumnFactoryInputListForm(this);
        columnListPanel = (JPanel) columnListForm.getComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
