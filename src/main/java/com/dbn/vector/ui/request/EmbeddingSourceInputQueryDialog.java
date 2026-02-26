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
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class EmbeddingSourceInputQueryDialog extends DBNDialog<EmbeddingSourceInputQueryForm> {
    private final EmbeddingSourceQuery sourceQuery;
    private final RecordOperation operation;


    public EmbeddingSourceInputQueryDialog(ConnectionHandler connection, EmbeddingSourceQuery config, RecordOperation operation) {
        super(connection, getDialogTitle(operation), false);
        this.sourceQuery = config;
        this.operation = operation;

        init();
    }

    private static String getDialogTitle(RecordOperation operation) {
        return operation == RecordOperation.CREATE ? "Add Source Query" :
                operation == RecordOperation.UPDATE ? "Update Source Query" :
                "Source Query";
    }


    @Override
    protected @NotNull EmbeddingSourceInputQueryForm createForm() {
        return new EmbeddingSourceInputQueryForm(this, ensureConnection(), sourceQuery);
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
