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

package com.dbn.vector.ui.result;

import com.dbn.common.data.Data;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTableGutterModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.vector.model.result.EmbeddingResult;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListModel;
import java.util.List;

import static com.dbn.common.util.Lists.isInBounds;

@Getter
public class VectorEmbeddingSourcesTableModel extends DBNMutableTableModel<EmbeddingResult> implements DBNTableWithGutterModel<EmbeddingResult>, DatabaseContextBase {
    private final ListModel gutterModel = new DBNTableGutterModel<>(this);
    private final List<EmbeddingResult> embeddingResults;

    // Column identifiers
    public static final String COL_SOURCE_NAME = "Source name";
    public static final String COL_SOURCE_SIZE = "Source size";
    public static final String COL_ROWS_EMBEDDED = "Rows embedded";
    public static final String COL_STATUS = "Status";

    private static final String[] COLUMN_NAMES = {
            COL_SOURCE_NAME,
            COL_SOURCE_SIZE,
            COL_ROWS_EMBEDDED,
            COL_STATUS,
    };

    public VectorEmbeddingSourcesTableModel(List<EmbeddingResult> embeddingResults) {
        this.embeddingResults = embeddingResults;
    }

    @Nullable
    public ConnectionHandler getConnection() {
        return null;
    }

    @Override
    public int getRowCount() {
        return embeddingResults.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (isInBounds(embeddingResults, rowIndex)) {
            return embeddingResults.get(rowIndex);
        }
        return null;
    }

    @Override
    public Object getValue(EmbeddingResult row, int column) {
        if (row == null) return null;
        return switch (column) {
            case 0 -> row.getName();
            case 1 -> row.getPresentableSize();
            case 2 -> row.getRowsInserted();
            case 3 -> row.getStatus();
            default -> "";
        };
    }

    @Override
    public String getPresentableValue(EmbeddingResult row, int column) {
        return Data.asString(getValue(row, column));
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public @NotNull Class<?> getColumnClass(int columnIndex) {
        return EmbeddingResult.class;
    }

    @Override
    public void disposeInner() {
        // Clean up if needed (e.g. unregister listeners), but no-op here.
    }
}