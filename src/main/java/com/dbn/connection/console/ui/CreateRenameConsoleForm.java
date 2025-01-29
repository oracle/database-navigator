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

package com.dbn.connection.console.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBConsole;
import com.dbn.vfs.DBConsoleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.util.Strings.isNotEmpty;

public class CreateRenameConsoleForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JTextField consoleNameTextField;

    private final ConnectionRef connection;
    private final DBConsoleType consoleType;
    private final DBConsole console;

    CreateRenameConsoleForm(final CreateRenameConsoleDialog parent, @NotNull ConnectionHandler connection, @Nullable final DBConsole console, DBConsoleType consoleType) {
        super(parent);
        this.connection = connection.ref();
        this.console = console;
        this.consoleType = consoleType;

        DBNHeaderForm headerForm = console == null ?
                new DBNHeaderForm(this, "[New " + consoleType.getName() + "]", consoleType.getIcon(), connection.getEnvironmentType().getColor()) :
                new DBNHeaderForm(this, console);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        Set<String> consoleNames = connection.getConsoleBundle().getConsoleNames();

        String name;
        if (console == null) {
            name = connection.getName() + " 1";
            while (consoleNames.contains(name)) {
                name = Naming.nextNumberedIdentifier(name, true);
            }
        } else {
            name = console.getName();
            consoleNames.remove(name);
            parent.getOKAction().setEnabled(false);
        }
        consoleNameTextField.setText(name);
    }

    @Override
    protected void initValidation() {
        formValidator.addTextValidation(consoleNameTextField, n-> isNotEmpty(n), "Console name must be specified");
        formValidator.addTextValidation(consoleNameTextField, n-> isNotUsed(n), "Console name already in use");
    }

    private boolean isNotUsed(String name) {
        if (console != null && Objects.equals(console.getName(), name)) return true;

        Set<String> consoleNames = getConnection().getConsoleBundle().getConsoleNames();
        return !consoleNames.contains(name);
    }


    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return consoleNameTextField;
    }

    public String getConsoleName() {
        return consoleNameTextField.getText();
    }

    public DBConsoleType getConsoleType() {
        return console == null ? consoleType : console.getConsoleType();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public DBConsole getConsole() {
        return console;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
