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

import com.dbn.common.ui.list.MutableObjectList;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.ui.request.bulk.TableSelectionDialog;

import java.util.List;

import static com.dbn.common.operation.RecordOperation.CREATE;
import static com.dbn.common.operation.RecordOperation.UPDATE;
import static com.dbn.common.ui.util.Mouse.onMouseDoubleClick;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Unsafe.cast;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

public class EmbeddingSourceTablesList extends MutableObjectList<EmbeddingSourceTable> {
    private final VectorEmbeddingRequest embeddingRequest;

    public EmbeddingSourceTablesList(List<EmbeddingSourceTable> tableSources, VectorEmbeddingRequest embeddingRequest) {
        super(new EmbeddingSourceTablesListModel(tableSources));
        setCellRenderer(new EmbeddingSourceTablesListRenderer());
        setVisibleRowCount(5);
        this.embeddingRequest = embeddingRequest;
        onMouseDoubleClick(this, e -> updateRow());
    }

    public void insertRows() {
        Dialogs.show(
                () -> new TableSelectionDialog(embeddingRequest.getConnection()),
                (dialog, exitCode) -> {
                    if (exitCode == OK_EXIT_CODE) {
                        List<EmbeddingSourceTable> selectedTables = dialog.getSelectedTableSources();
                        EmbeddingSourceTablesListModel model = getModel();
                        model.addAll(selectedTables);
                    }
                }
        );
    }

    public void insertRow() {
        EmbeddingSourceTable sourceTable = embeddingRequest.getSourceConfig().getSourceTable();
        ConnectionHandler connection = embeddingRequest.getConnection();
        EmbeddingSourceTablesListModel model = getModel();

        Dialogs.show(
                () -> new EmbeddingSourceInputTableDialog(connection, sourceTable, CREATE),
                whenOk(d -> {
                    model.add(sourceTable.clone());
                    // reset
                    sourceTable.setTableName(null);
                    sourceTable.setKeyColumnName(null);
                    sourceTable.setDataColumnName(null);
                }));
    }

    public void updateRow() {
        EmbeddingSourceTable sourceTable = getSelectedValue();
        if (sourceTable == null) return;

        ConnectionHandler connection = embeddingRequest.getConnection();
        Dialogs.show(() -> new EmbeddingSourceInputTableDialog(connection, sourceTable, UPDATE));
    }

    @Override
    public EmbeddingSourceTablesListModel getModel() {
        return cast(super.getModel());
    }

    public List<EmbeddingSourceTable> getTables() {
        return getModel().getElements();
    }
}
