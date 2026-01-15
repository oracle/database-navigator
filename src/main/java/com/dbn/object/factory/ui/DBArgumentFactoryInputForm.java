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
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.common.util.Strings.isWord;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_INPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_OUTPUT;

public class DBArgumentFactoryInputForm extends DBObjectFactoryInputForm<DBObjectSpec> {
    private JPanel mainPanel;
    private JLabel iconLabel;
    private JBTextField nameTextField;
    private JCheckBox inCheckBox;
    private JCheckBox outCheckBox;
    private JPanel dataTypeEditorPanel;

    private final DataTypeEditor dataTypeEditor;

    public DBArgumentFactoryInputForm(DBNComponent parent, DBObjectSpec input) {
        super(parent, input);
        iconLabel.setText(null);
        iconLabel.setIcon(enforceInArgument() ? Icons.DBO_ARGUMENT_IN : DBObjectType.ARGUMENT.getIcon());
        if (enforceInArgument()) {
            inCheckBox.setVisible(false);
            outCheckBox.setVisible(false);
        } else {
            inCheckBox.addActionListener(actionListener);
            outCheckBox.addActionListener(actionListener);
        }
        nameTextField.getEmptyText().setText("Argument name");

        dataTypeEditor = new DataTypeEditor(getConnection());
        dataTypeEditorPanel.add(dataTypeEditor);

        resetFormChanges();
    }

    private boolean enforceInArgument() {
        DBArgumentFactoryInputListForm argumentListForm = ensureParentFrom(DBArgumentFactoryInputListForm.class);
        DBMethodFactoryInputForm methodForm = argumentListForm.ensureParentComponent();
        return methodForm.enforceInArguments();
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> !isReadonlyInput(), array(
                nameTextField,
                dataTypeEditor,
                inCheckBox,
                outCheckBox));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), "Please enter an argument name");
        addTextValidation(nameTextField, n -> isWord(n), "Please enter a valid argument name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "Please enter a unique argument name");

        addTextValidation(getTypeTextField(), t -> isNotEmptyOrSpaces(t), "Please enter the argument data type");
    }

    private boolean isNotUsed(String argumentName) {
        DBArgumentFactoryInputListForm parentComponent = ensureParentComponent();
        Set<String> argumentNames = parentComponent.getObjectNames(this);
        return !argumentNames.contains(argumentName);
    }

    @Override
    protected void initAccessibility() {
        JTextField typeTextField = getTypeTextField();

        setAccessibleName(typeTextField, "Argument type");
        setAccessibleName(nameTextField, "Argument name");
        setAccessibleName(inCheckBox, "Is input argument");
        setAccessibleName(outCheckBox, "Is output argument");
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(nameTextField, dataTypeEditorPanel);
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

    private final ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == inCheckBox || e.getSource() == outCheckBox) {
                Icon icon =
                     inCheckBox.isSelected() && outCheckBox.isSelected() ? Icons.DBO_ARGUMENT_IN_OUT :
                     inCheckBox.isSelected() ? Icons.DBO_ARGUMENT_IN :
                     outCheckBox.isSelected() ? Icons.DBO_ARGUMENT_OUT : Icons.DBO_ARGUMENT;

                iconLabel.setIcon(icon);
            }
        }
    };

    @Override
    public void applyFormChanges() {
        input.setObjectName(getText(nameTextField));
        input.setAttributeValue(DATA_TYPE, dataTypeEditor.getDataTypeRepresentation());
        input.setAttributeValue(IS_INPUT, enforceInArgument() || inCheckBox.isSelected());
        input.setAttributeValue(IS_OUTPUT, outCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        nameTextField.setText(input.getObjectName());
        inCheckBox.setSelected(IS_INPUT.is(input));
        outCheckBox.setSelected(IS_OUTPUT.is(input));
        dataTypeEditor.setText(DATA_TYPE.of(input));
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
