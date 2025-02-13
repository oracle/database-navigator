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

package com.dbn.connection.session.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.session.DatabaseSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.util.Strings.isNotEmpty;

public class CreateRenameSessionForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JTextField sessionNameTextField;

    private final ConnectionRef connection;
    private DatabaseSession session;

    CreateRenameSessionForm(CreateRenameSessionDialog parent, @NotNull ConnectionHandler connection, @Nullable DatabaseSession session) {
        super(parent);
        this.connection = connection.ref();
        this.session = session;

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        Set<String> sessionNames = connection.getSessionBundle().getSessionNames();

        String name;
        if (session == null) {
            name = "Session 1";
            while (sessionNames.contains(name)) {
                name = Naming.nextNumberedIdentifier(name, true);
            }
        } else {
            name = session.getName();
            sessionNames.remove(name);
            parent.getOKAction().setEnabled(false);
        }
        sessionNameTextField.setText(name);
    }

    @Override
    protected void initValidation() {
        addTextValidation(sessionNameTextField, n-> isNotEmpty(n), "Session name must be specified");
        addTextValidation(sessionNameTextField, n-> isNotUsed(n), "Session name already in use");
    }

    private boolean isNotUsed(String name) {
        if (session != null && Objects.equals(session.getName(), name)) return true;

        Set<String> sessionNames = getConnection().getSessionBundle().getSessionNames();
        return !sessionNames.contains(name);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return sessionNameTextField;
    }

    public String getSessionName() {
        return sessionNameTextField.getText();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public DatabaseSession getSession() {
        return session;
    }

    public void setSession(DatabaseSession session) {
        this.session = session;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
