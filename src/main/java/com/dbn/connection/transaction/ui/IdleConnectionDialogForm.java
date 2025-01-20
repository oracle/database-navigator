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

package com.dbn.connection.transaction.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

public class IdleConnectionDialogForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTextPane hintTextPane;

    public IdleConnectionDialogForm(DBNDialog parent, ConnectionHandler connection, DBNConnection conn, int timeoutMinutes) {
        super(parent);
        int idleMinutes = conn.getIdleMinutes();
        int idleMinutesToDisconnect = connection.getSettings().getDetailSettings().getIdleMinutesToDisconnect();

        String text = "The connection \"" + connection.getConnectionName(conn) + "\" is been idle for more than " + idleMinutes + " minutes. You have uncommitted changes on this connection. " +
                "Please specify whether to commit or rollback the changes. You can choose to keep the connection alive for another " + idleMinutesToDisconnect + " more minutes. \n\n" +
                "NOTE: Connection will close automatically and changes will be rolled-back if this prompt stays unattended for more than " + timeoutMinutes + " minutes.";
        hintTextPane.setBackground(mainPanel.getBackground());
        hintTextPane.setText(text);


        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
