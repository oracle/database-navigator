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
import com.dbn.common.ui.form.DBNForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ArgumentFactoryInputForm extends ObjectFactoryInputForm<ArgumentFactoryInput> {
    private JPanel mainPanel;
    private JLabel iconLabel;
    private JTextField nameTextField;
    private JCheckBox inCheckBox;
    private JCheckBox outCheckBox;
    private JPanel dataTypeEditor;
    private final boolean enforceInArgument;

    ArgumentFactoryInputForm(DBNForm parent, ConnectionHandler connection, boolean enforceInArgument, int index) {
        super(parent, connection, DBObjectType.ARGUMENT, index);
        this.enforceInArgument = enforceInArgument;
        iconLabel.setText(null);
        iconLabel.setIcon(enforceInArgument ? Icons.DBO_ARGUMENT_IN : DBObjectType.ARGUMENT.getIcon());
        if (enforceInArgument) {
            inCheckBox.setVisible(false);
            outCheckBox.setVisible(false);
        } else {
            inCheckBox.addActionListener(actionListener);
            outCheckBox.addActionListener(actionListener);
        }
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
    public ArgumentFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        return new ArgumentFactoryInput(
                parent,
                getIndex(),
                nameTextField.getText(),
                ((DataTypeEditor) dataTypeEditor).getDataTypeRepresentation(),
                enforceInArgument || inCheckBox.isSelected(),
                outCheckBox.isSelected());
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
}
