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

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.object.factory.model.DBColumnSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Set;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;

public class DBColumnFactoryInputForm extends DBObjectFactoryInputForm<DBColumnSpec> {
    private JPanel mainPanel;
    private JLabel iconLabel;
    private JBTextField nameTextField;
    private JCheckBox notNullCheckBox;
    private JCheckBox primaryKeyCheckBox;
    private JPanel dataTypeEditorPanel;

    private final DataTypeEditor dataTypeEditor;

    public DBColumnFactoryInputForm(DBNComponent parent, DBColumnSpec input) {
        super(parent, input);
        iconLabel.setText(null);
        iconLabel.setIcon(DBObjectType.COLUMN.getIcon());

        nameTextField.getEmptyText().setText("Column name");
        dataTypeEditor = new DataTypeEditor(getConnection());
        dataTypeEditorPanel.add(dataTypeEditor);

        resetFormChanges();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> !isReadonly(), array(
                nameTextField,
                dataTypeEditor,
                notNullCheckBox,
                primaryKeyCheckBox));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter a column name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid column name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "Please enter a unique column name");

        addTextValidation(getTypeTextField(), t -> isNotEmptyOrSpaces(t), "Please enter the column data type");
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(nameTextField, dataTypeEditorPanel);
    }

    private boolean isNotUsed(String columnName) {
        DBColumnFactoryInputListForm parentComponent = ensureParentFrom(DBColumnFactoryInputListForm.class);
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
        return dataTypeEditor.getTextField();
    }

    @Override
    protected String getSchemaName() {
        return null; // TODO only used for header panel (check if relevant here)
    }

    @Override
    protected String getObjectName() {
        return getText(nameTextField);
    }

    @Override
    public void applyFormChanges() {
        input.setObjectName(getText(nameTextField));
        input.setDataType(dataTypeEditor.getDataTypeRepresentation());
        input.setNonNull(notNullCheckBox.isSelected());
        input.setPrimaryKey(primaryKeyCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(input.getObjectName());
        dataTypeEditor.setText(input.getDataType());
        notNullCheckBox.setSelected(input.isNonNull());
        primaryKeyCheckBox.setSelected(input.isPrimaryKey());
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

    @Override
    public void disposeInner() {
        removeValidators(nameTextField);
        removeValidators(getTypeTextField());
        super.disposeInner();
    }
}
