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

package com.dbn.editor.data.nested;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.object.DBNestedTable;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class DBNestedTableViewerDialog extends DBNDialog<DBNestedTableViewerForm> {
    private final DBObjectRef<DBNestedTable> nestedTable;

    public DBNestedTableViewerDialog(@NotNull DBNestedTable nestedTable) {
        super(nestedTable.getConnection(), txt("app.objects.action.ViewData"), true);
        this.nestedTable = DBObjectRef.of(nestedTable);
        setModal(false);
        setResizable(true);
        setDefaultSize(1000, 650);
        init();
    }

    public static void show(@NotNull DBNestedTable nestedTable) {
        Dialogs.show(() -> new DBNestedTableViewerDialog(nestedTable));
    }

    @NotNull
    @Override
    protected DBNestedTableViewerForm createForm() {
        return new DBNestedTableViewerForm(this, DBObjectRef.ensure(nestedTable));
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
