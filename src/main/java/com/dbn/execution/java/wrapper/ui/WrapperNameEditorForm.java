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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.util.ui.JBUI;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.ui.util.TextFields.onTextChange;

public class WrapperNameEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel objectIconLabel;
    private JTextField objectNameTextField;

    private final DBObjectRef object;

    public WrapperNameEditorForm(WrapperNamesEditorForm parent, DBObjectRef object) {
        super(parent);
        this.object = object;

        initIconLabel();
        initNameField();
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
            String objectName = objectNameTextField.getText().trim();
            object.setObjectName(objectName);
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected void initValidation() {
        WrapperNamesEditorForm providerForm = ensureParentComponent();
        int maxIdentifierLength = providerForm.getMaxIdentifierLength();
        addTextValidation(objectNameTextField, p -> p.length() <= maxIdentifierLength, "Identifier length should not exceed " + maxIdentifierLength + " characters");
    }
}
