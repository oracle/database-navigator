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

package com.dbn.connection.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.util.Arrays;

import static com.dbn.common.ui.util.PasswordFields.getPassword;
import static com.dbn.common.ui.util.PasswordFields.testPassword;
import static com.dbn.common.util.Chars.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionPasswordChangeForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    ConnectionPasswordChangeForm(@NotNull ConnectionPasswordChangeDialog parentComponent, @NotNull ConnectionHandler connection) {
        super(parentComponent);

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    @Override
    protected void initValidation() {
        addPasswordValidation(currentPasswordField, p -> isNotEmpty(p), txt("msg.connection.error.CurrentPasswordRequired"));
        addPasswordValidation(newPasswordField, p -> isNotEmpty(p), txt("msg.connection.error.NewPasswordRequired"));
        addPasswordValidation(confirmPasswordField, this::isNewPasswordConfirmed, txt("msg.connection.error.NewPasswordMismatch"));
    }

    private boolean isNewPasswordConfirmed(char[] confirmPassword) {
        return testPassword(newPasswordField, password -> Arrays.equals(password, confirmPassword));
    }

    public char[] getCurrentPassword() {
        return getPassword(currentPasswordField);
    }

    public char[] getNewPassword() {
        return getPassword(newPasswordField);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return currentPasswordField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
