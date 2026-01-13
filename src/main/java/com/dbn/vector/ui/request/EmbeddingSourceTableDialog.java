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

package com.dbn.vector.ui.request;

import com.dbn.common.operation.RecordOperation;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class EmbeddingSourceTableDialog extends DBNDialog<EmbeddingSourceTableForm> {
    private final EmbeddingSourceTable sourceTable;
    private final RecordOperation operation;


    public EmbeddingSourceTableDialog(ConnectionHandler connection, EmbeddingSourceTable config, RecordOperation operation) {
        super(connection, getDialogTitle(operation), false);
        this.sourceTable = config;
        this.operation = operation;

        init();
    }

    private static String getDialogTitle(RecordOperation operation) {
        return operation == RecordOperation.CREATE ? "Add Source Table" :
                operation == RecordOperation.UPDATE ? "Update Source Table" :
                "Source Table";
    }


    @Override
    protected @NotNull EmbeddingSourceTableForm createForm() {
        return new EmbeddingSourceTableForm(this, ensureConnection(), sourceTable);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), operation == RecordOperation.CREATE ? "Add" : "Update");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    @SneakyThrows
    protected void doOKAction() {
        applyFormChanges();
        super.doOKAction();
    }

    @Override
    @SneakyThrows
    public void doCancelAction() {
        if (operation == RecordOperation.CREATE) {
            applyFormChanges(); // preserve input even if canceled
        }
        super.doCancelAction();
    }
}
