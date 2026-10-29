/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.management.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.connection.ConnectionRef;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.ObjectFactoryIdentifiers.adjustIdentifierCase;

public class ObjectRenameForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTextField nameTextField;
    private JCheckBox preserveCaseCheckBox;
    private DBNInfoLabel preserveCaseInfoLabel;
    private final ConnectionRef connection;
    private final DatabaseIdentifierCase defaultCase;
    private final String currentName;

    ObjectRenameForm(ObjectRenameDialog parent, DBObject object) {
        super(parent);
        connection = object.getConnection().ref();
        defaultCase = object.getConnection().getCompatibility().getIdentifierCase();
        currentName = object.getName();
        preserveCaseCheckBox.setSelected(!currentName.equals(
                adjustIdentifierCase(connection.ensure(), currentName, defaultCase)));
        String infoText = txt("app.objects.tooltip.PreserveIdentifierCase",
                object.getConnection().getDatabaseType().getName(), defaultCase);
        preserveCaseInfoLabel.setContent(TextContent.htmlTooltip(infoText, "width:200px"));
        DBNHeaderForm header = new DBNHeaderForm(this, object);
        headerPanel.add(header.getComponent(), BorderLayout.CENTER);
        nameTextField.setText(currentName);
        nameTextField.selectAll();
        nameTextField.setToolTipText(txt("app.objects.tooltip.RenameObject"));
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmptyOrSpaces(n), txt("msg.objects.error.RenameNameRequired"));
        addTextValidation(nameTextField, n -> !adjustName(n.trim()).equals(currentName), txt("msg.objects.error.RenameNameUnchanged"));
        addTextValidation(nameTextField, n -> n.indexOf('\0') < 0, txt("msg.objects.error.RenameNameInvalid"));
    }

    @Override
    protected void initEventListeners() {
        preserveCaseCheckBox.addActionListener(e -> validateFormFields());
    }

    public String getNewName() {
        return adjustName(getText(nameTextField).trim());
    }

    private String adjustName(String name) {
        DatabaseIdentifierCase requestedCase = preserveCaseCheckBox.isSelected() ?
                DatabaseIdentifierCase.PRESERVE : defaultCase;
        return adjustIdentifierCase(connection.ensure(), name, requestedCase);
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @Override
    public @NotNull JPanel getMainComponent() {
        return mainPanel;
    }
}
