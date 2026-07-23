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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNDynamicTableCellRenderer;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.table.DBNTableWithGutter;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Borderless.markBorderless;
import static com.dbn.nls.NlsResources.txt;

public class VectorEmbeddingResultsTable extends DBNTableWithGutter<VectorEmbeddingResultsTableModel> {

    public VectorEmbeddingResultsTable(@NotNull DBNComponent parent, VectorEmbeddingResultsTableModel sources) {
        super(parent, sources, true);
        setCellSelectionEnabled(true);
        setDefaultRenderer(Object.class, new DBNDynamicTableCellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();
        markBorderless(this);

        setProportionalColumnWidth(0, 20);
        setProportionalColumnWidth(getModel().getColumnCount() -1, 30); // status message
        setAccessibleName(this, txt("app.vectors.aria.VectorEmbeddingResults"));
    }

    @Override
    protected DBNTableGutter<?> createTableGutter() {
        return new DBNTableGutter<DBNTableWithGutter>(this);
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }
}
