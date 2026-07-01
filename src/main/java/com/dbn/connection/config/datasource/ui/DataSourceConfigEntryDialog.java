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

import com.dbn.common.outcome.DialogCloseOutcomeHandler;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class DataSourceConfigEntryDialog extends DBNDialog<DataSourceConfigEntryForm> {
    @Nullable private final DBObjectRef<DBDataSourceConfigEntry> entry;
    @Nullable private final String value;

    public DataSourceConfigEntryDialog(@NotNull ConnectionHandler connection) {
        super(connection, txt("msg.objects.title.CreateObject", entryTypeName()), true);
        this.entry = null;
        this.value = null;
        initDialog();
    }

    public DataSourceConfigEntryDialog(@NotNull DBDataSourceConfigEntry entry, @NotNull String value) {
        super(entry.getConnection(), txt("msg.objects.title.EditObject", entryTypeName()), true);
        this.entry = DBObjectRef.of(entry);
        this.value = value;
        initDialog();
    }

    private static String entryTypeName() {
        return DBObjectType.DATA_SOURCE_CONFIG_ENTRY.getTitleCasedDisplayName();
    }

    private void initDialog() {
        setModal(true);
        setResizable(true);
        setDefaultSize(900, 680);
        init();
    }

    @Override
    protected @NotNull DataSourceConfigEntryForm createForm() {
        DBDataSourceConfigEntry entry = getEntry();
        return entry == null ?
                new DataSourceConfigEntryForm(this, ensureConnection()) :
                new DataSourceConfigEntryForm(this, entry, value);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt(isEdit() ? "msg.shared.button.Update" : "msg.shared.button.Create"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        OutcomeHandler closeHandler = DialogCloseOutcomeHandler.create(this);
        if (isEdit()) {
            getForm().updateEntry(closeHandler);
        } else {
            getForm().createEntry(closeHandler);
        }
    }

    private boolean isEdit() {
        return getEntry() != null;
    }

    private @Nullable DBDataSourceConfigEntry getEntry() {
        return DBObjectRef.get(entry);
    }
}
