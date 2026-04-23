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

package com.dbn.connection.config.datasource.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.datasource.service.DataSourceConfigStoreService;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import java.awt.event.ActionEvent;

public class DataSourceConfigManagerDialog extends DBNDialog<DataSourceConfigManagerForm> {
    private JButton applyButton;

    public DataSourceConfigManagerDialog(@NotNull ConnectionHandler connection) {
        super(connection, "Data Source Config Manager", true);
        setResizable(true);
        setModal(true);
        setDefaultSize(1180, 760);
        init();
    }

    @Override
    protected @NotNull DataSourceConfigManagerForm createForm() {
        return new DataSourceConfigManagerForm(
                this,
                ensureConnection(),
                new DataSourceConfigStoreService(),
                this::updateActionState);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "OK");
        return actions(getOKAction(), new ApplyAction(), getCancelAction());
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        if (action instanceof ApplyAction) {
            applyButton = new JButton();
            applyButton.setAction(action);
            applyButton.setEnabled(false);
            return applyButton;
        }
        return super.createJButtonForAction(action);
    }

    @Override
    public void doOKAction() {
        if (getForm().hasPendingChanges()) {
            boolean applied = getForm().applyChanges();
            if (!applied) return;
        }
        super.doOKAction();
    }

    public void updateActionState() {
        if (applyButton != null) {
            applyButton.setEnabled(getForm().hasPendingChanges());
        }
    }

    private class ApplyAction extends AbstractAction {
        ApplyAction() {
            renameAction(this, "Apply");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getForm().applyChanges()) {
                updateActionState();
            }
        }
    }
}
