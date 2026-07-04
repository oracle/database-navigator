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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.Resources;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.credentials.Secret;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.ui.ExitActionType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionPasswordChangeDialog extends DBNDialog<ConnectionPasswordChangeForm> {
    private final ConnectionSettings connectionSettings;

    public ConnectionPasswordChangeDialog(@NotNull ConnectionSettings connectionSettings) {
        super(connectionSettings.getProject(), txt("msg.connection.title.ChangePassword"), true);
        this.connectionSettings = connectionSettings;
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected ConnectionPasswordChangeForm createForm() {
        return new ConnectionPasswordChangeForm(this, connectionSettings);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.connection.button.ChangePassword"));
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        ConnectionPasswordChangeForm form = getForm();
        char[] currentPassword = form.getCurrentPassword();
        char[] newPassword = form.getNewPassword();
        getOKAction().setEnabled(false);

        Progress.modal(getProject(), null, true,
                txt("prc.connection.title.ChangingPassword"),
                txt("prc.connection.text.ChangingPassword", connectionSettings.getDatabaseSettings().getDisplayName()),
                progress -> changePassword(currentPassword, newPassword));
    }

    private void changePassword(char[] currentPassword, char[] newPassword) {
        try {
            AuthenticationInfo authenticationInfo = connectionSettings.getDatabaseSettings().getAuthenticationInfo().clone();
            authenticationInfo.setPassword(currentPassword);

            DBNConnection connection = ConnectionUtil.changePassword(
                    connectionSettings,
                    null,
                    authenticationInfo,
                    SessionId.TEST,
                    false,
                    newPassword);
            Resources.close(connection);

            updateStoredPassword(newPassword);
            dispatch(() -> completePasswordChange());
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            dispatch(() -> getOKAction().setEnabled(true));
        } catch (Exception e) {
            conditionallyLog(e);
            dispatch(() -> {
                getOKAction().setEnabled(true);
                Messages.showErrorDialog(getProject(), txt("msg.connection.title.ChangePassword"), getLocalizedMessage(e));
            });
        } finally {
            clearPassword(currentPassword);
            clearPassword(newPassword);
        }
    }

    private void completePasswordChange() {
        close(OK_EXIT_CODE, ExitActionType.OK);
        Messages.showInfoDialog(
                getProject(),
                txt("msg.connection.title.PasswordChanged"),
                txt("msg.connection.confirmation.PasswordChanged", connectionSettings.getDatabaseSettings().getDisplayName()));
    }

    private void updateStoredPassword(char[] newPassword) {
        ConnectionId connectionId = connectionSettings.getConnectionId();
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        AuthenticationInfo authenticationInfo = connection == null ?
                connectionSettings.getDatabaseSettings().getAuthenticationInfo() :
                connection.getAuthenticationInfo();
        Secret[] oldSecrets = authenticationInfo.snapshotSecrets();
        authenticationInfo.setPassword(newPassword);
        authenticationInfo.updateSecrets(oldSecrets);

        if (connection == null) return;

        connection.getConnectionStatus().setAuthenticationError(null);
        connection.closeAllConnections();
    }
}
