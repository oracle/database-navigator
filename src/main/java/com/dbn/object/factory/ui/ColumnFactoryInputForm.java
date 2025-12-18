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

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.object.factory.ColumnFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.factory.ui.common.ObjectListForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Set;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;

public class ColumnFactoryInputForm extends ObjectFactoryInputForm<ColumnFactoryInput> {
    private JPanel mainPanel;
    private JLabel iconLabel;
    private JBTextField nameTextField;
    private JPanel dataTypeEditor;

    ColumnFactoryInputForm(ColumnFactoryInputListForm parent, ConnectionHandler connection, int index, @Nullable ObjectListForm.ObjectDetail detail) {
        super(parent, connection, DBObjectType.COLUMN, index);
        iconLabel.setText(null);
        iconLabel.setIcon(DBObjectType.COLUMN.getIcon());

        nameTextField.getEmptyText().setText("Column name");
        getDataTypeEditor().setText(detail == null ? "" : detail.getName());
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a column name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid column name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "Please enter a unique column name");

        addTextValidation(getTypeTextField(), t -> isNotEmptyOrSpaces(t), "Please enter the column data type");
    }

    private boolean isNotUsed(String columnName) {
        ColumnFactoryInputListForm parentComponent = ensureParentComponent();
        Set<String> columnNames = parentComponent.getObjectNames(this);
        return !columnNames.contains(columnName);
    }

    @Override
    protected void initAccessibility() {
        JTextField typeTextField = getTypeTextField();

        setAccessibleName(typeTextField, "Column type");
        setAccessibleName(nameTextField, "Column name");
    }

    private JBTextField getTypeTextField() {
        return getDataTypeEditor().getTextField();
    }

    @Override
    public String getObjectName() {
        return getText(nameTextField);
    }


    @Override
    public ColumnFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        return new ColumnFactoryInput(
                parent,
                getIndex(),
                getText(nameTextField),
                getDataTypeEditor().getDataTypeRepresentation());
    }

    @Override
    public void restoreUserInput(@Nullable ColumnFactoryInput input) {
        if (input == null) return;

        nameTextField.setText(input.getObjectName());
        getDataTypeEditor().setText(input.getDataType());
    }

    private DataTypeEditor getDataTypeEditor() {
        return (DataTypeEditor) dataTypeEditor;
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

    private void createUIComponents() {
        dataTypeEditor = new DataTypeEditor(getConnection());
    }

    @Override
    public void disposeInner() {
        removeValidators(nameTextField);
        removeValidators(getTypeTextField());
        super.disposeInner();
    }
}
