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

package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.util.Set;

import static com.dbn.common.ui.alignment.FieldAligner.alignFormFields;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;

public class WrapperNameEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel objectIconLabel;
    private JTextField objectNameTextField;
    private JLabel statusLabel;

    private final DBObjectRef object;

    public WrapperNameEditorForm(WrapperNamesEditorForm parent, DBObjectRef object) {
        super(parent);
        this.object = object;

        initIconLabel();
        initNameField();
        initStatusLabel();
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(objectIconLabel, objectNameTextField, statusLabel);
    }

    private void initStatusLabel() {
        statusLabel.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        statusLabel.setHorizontalAlignment(JLabel.RIGHT);
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        String identifier = getText(objectNameTextField);
        int length = identifier.length();
        int maxLength = getMaxIdentifierLength();

        statusLabel.setText(length + " chars");

        Color foreground = length > 0 && length <= maxLength ?
                Colors.faded(UIUtil.getLabelForeground()) :
                Colors.getLabelErrorForeground();
        statusLabel.setForeground(foreground);
    }

    private void initIconLabel() {
        objectIconLabel.setIcon(object.getObjectType().getIcon());
        objectIconLabel.setText(null);
        if (object.getParentRef().getObjectType() != DBObjectType.SCHEMA) {
            objectIconLabel.setBorder(JBUI.Borders.empty(0, 48, 0, 0));
        }
    }

    private void initNameField() {
        objectNameTextField.setText(object.getObjectName(false));

        onTextChange(objectNameTextField, e -> {
            String objectName = getText(objectNameTextField);
            object.setObjectName(objectName);
            updateStatusLabel();

            WrapperNamesEditorForm providerForm = ensureParentComponent();
            alignFormFields(providerForm);
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initValidation() {
        int maxLength = getMaxIdentifierLength();
        addTextValidation(objectNameTextField, p -> isNotEmptyOrSpaces(p), "Identifier cannot be empty");
        addTextValidation(objectNameTextField, p -> p.trim().length() <= maxLength, "Identifier length cannot exceed " + maxLength + " characters");
        addTextValidation(objectNameTextField, p -> p.trim().matches("^[a-zA-Z][a-zA-Z0-9_$#]*$"), "Identifiers can only contain alphanumeric characters, underscores, dollar and hash signs");
        addTextValidation(objectNameTextField, p -> isUniqueIdentifier(), "Identifier names must be unique");
    }

    private boolean isUniqueIdentifier() {
        WrapperNamesEditorForm providerForm = ensureParentComponent();
        Set<String> identifierNames = providerForm.getIdentifierNames(this);
        return identifierNames.stream().noneMatch(n -> n.equalsIgnoreCase(getIdentifierName()));
    }

    public String getIdentifierName() {
        return getText(objectNameTextField);
    }

    private int getMaxIdentifierLength() {
        WrapperNamesEditorForm providerForm = ensureParentComponent();
        return providerForm.getMaxIdentifierLength();
    }
}
