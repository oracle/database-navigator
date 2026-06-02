/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.vector.ui.request.bulk;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.nls.NlsResources.txt;

public class TableSelectionDialog extends DBNDialog<TableSelectionForm> {
    // Store selected tables BEFORE dialog closes (components get disposed after close)
    private List<EmbeddingSourceTable> selectedTableSources = new ArrayList<>();

    public TableSelectionDialog(@NotNull ConnectionHandler connection) {
        super(connection, txt("msg.vector.title.AddTables"), true);
        setDefaultSize(650, 550);
        init();
    }

    @Override
    @NotNull
    protected TableSelectionForm createForm() {
        return new TableSelectionForm(this, ensureConnection());
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Add Selected");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        TableSelectionForm form = getForm();

        // Validate - all selected tables must have column config
        if (!form.isValid()) {
            List<String> missing = form.getTablesWithoutConfig();
            showWarningDialog(
                    getProject(),
                    txt("msg.vector.title.MissingConfiguration"),
                    txt("msg.vector.message.MissingColumnConfig", String.join(", ", missing))
            );
            return;
        }

        // Capture selected tables BEFORE closing (components will be disposed after close)
        this.selectedTableSources = form.getSelectedTableSources();

        super.doOKAction();
    }

    /**
     * Get the configured table sources after dialog closes.
     * This returns the cached list that was captured before the dialog closed.
     */
    public List<EmbeddingSourceTable> getSelectedTableSources() {
        return selectedTableSources;
    }
}
