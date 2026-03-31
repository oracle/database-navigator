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
import com.dbn.common.util.UUIDs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingSourceQuery;

import java.util.List;

import static com.dbn.common.operation.RecordOperation.CREATE;
import static com.dbn.common.operation.RecordOperation.UPDATE;
import static com.dbn.common.ui.util.Mouse.onMouseDoubleClick;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Unsafe.cast;

public class EmbeddingSourceQueriesList extends MutableObjectList<EmbeddingSourceQuery> {
    private final VectorEmbeddingRequest embeddingRequest;

    public EmbeddingSourceQueriesList(List<EmbeddingSourceQuery> sourceQueries, VectorEmbeddingRequest embeddingRequest) {
        super(new EmbeddingSourceQueriesListModel(sourceQueries));
        setCellRenderer(new EmbeddingSourceQueriesListRenderer());
        setVisibleRowCount(5);
        this.embeddingRequest = embeddingRequest;
        onMouseDoubleClick(this, e -> updateRow());
    }

    public void insertRow() {
        ConnectionHandler connection = embeddingRequest.getConnection();
        EmbeddingSourceQuery sourceQuery = embeddingRequest.getSourceConfig().getSourceQuery();
        Dialogs.show(
                () -> new EmbeddingSourceInputQueryDialog(connection, sourceQuery, CREATE),
                whenOk(d -> createSourceQuery()));
    }

    private void createSourceQuery() {
        // clone the transient source query
        EmbeddingSourceQuery sourceQuery = embeddingRequest.getSourceConfig().getSourceQuery();
        EmbeddingSourceQueriesListModel model = getModel();
        EmbeddingSourceQuery sourceQueryClone = sourceQuery.clone();
        model.add(sourceQueryClone);

        // reset
        sourceQueryClone.setIdentifier(UUIDs.compact());
        sourceQuery.setSelectStatement(null);
    }

    public void updateRow() {
        EmbeddingSourceQuery sourceQuery = getSelectedValue();
        if (sourceQuery == null) return;

        ConnectionHandler connection = embeddingRequest.getConnection();
        Dialogs.show(() -> new EmbeddingSourceInputQueryDialog(connection, sourceQuery, UPDATE));
    }

    @Override
    public EmbeddingSourceQueriesListModel getModel() {
        return cast(super.getModel());
    }

    public List<EmbeddingSourceQuery> getQueries() {
        return getModel().getElements();
    }
}
