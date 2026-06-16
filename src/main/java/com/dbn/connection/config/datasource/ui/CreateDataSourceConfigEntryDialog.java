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

import javax.swing.Action;

public class CreateDataSourceConfigEntryDialog extends DBNDialog<CreateDataSourceConfigEntryForm> {

    public CreateDataSourceConfigEntryDialog(@NotNull ConnectionHandler connection) {
        super(connection, "Create Configuration Entry", true);
        setModal(true);
        setResizable(true);
        setDefaultSize(900, 680);
        init();
    }

    @Override
    protected @NotNull CreateDataSourceConfigEntryForm createForm() {
        return new CreateDataSourceConfigEntryForm(
                this,
                ensureConnection(),
                new DataSourceConfigStoreService());
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Create");
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().createEntry(super::doOKAction);
    }

    void setActionsEnabled(boolean enabled) {
        getOKAction().setEnabled(enabled);
        getCancelAction().setEnabled(enabled);
    }
}
