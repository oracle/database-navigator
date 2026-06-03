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
import com.dbn.object.factory.model.DBObjectSpec;
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
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_NOT_NULL;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_PRIMARY_KEY;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_NAME;

public class DBColumnFactoryInputForm extends DBObjectFactoryInputForm {
    private JPanel mainPanel;
    private JLabel iconLabel;
    private JBTextField nameTextField;
    private JCheckBox notNullCheckBox;
    private JCheckBox primaryKeyCheckBox;
    private JPanel dataTypeEditorPanel;

    private final DataTypeEditor dataTypeEditor;

    public DBColumnFactoryInputForm(DBNComponent parent, DBObjectSpec input) {
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
        fieldAdapter.initFieldsAvailability(() -> !isReadonlyAttribute(OBJECT_NAME), array(nameTextField));
        fieldAdapter.initFieldsAvailability(() -> !isReadonlyAttribute(DATA_TYPE), array(dataTypeEditor));
        fieldAdapter.initFieldsAvailability(() -> !isReadonlyAttribute(IS_NOT_NULL), array(notNullCheckBox));
        fieldAdapter.initFieldsAvailability(() -> !isReadonlyAttribute(IS_PRIMARY_KEY), array(primaryKeyCheckBox));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), txt("msg.objects.error.ColumnNameRequired"));
        addTextValidation(nameTextField, n -> isWord(n), txt("msg.objects.error.ValidColumnNameRequired"));
        addTextValidation(nameTextField, n -> isNotUsed(n), txt("msg.objects.error.UniqueColumnNameRequired"));

        addTextValidation(getTypeTextField(), t -> isNotEmptyOrSpaces(t), txt("msg.objects.error.ColumnDataTypeRequired"));
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

        setAccessibleName(typeTextField, txt("app.objects.aria.ColumnType"));
        setAccessibleName(nameTextField, txt("app.objects.aria.ColumnName"));
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
        input.setAttributeValue(DATA_TYPE, dataTypeEditor.getDataTypeRepresentation());
        input.setAttributeValue(IS_NOT_NULL, notNullCheckBox.isSelected());
        input.setAttributeValue(IS_PRIMARY_KEY, primaryKeyCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(input.getObjectName());
        dataTypeEditor.setText(DATA_TYPE.of(input));
        notNullCheckBox.setSelected(IS_NOT_NULL.is(input));
        primaryKeyCheckBox.setSelected(IS_PRIMARY_KEY.is(input));
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
