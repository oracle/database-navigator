/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.data.model.resultSet;

import com.dbn.common.collections.CompactArrayList;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.sortable.SortableDataModelRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


public class ResultSetDataModelRow<
        M extends ResultSetDataModel<? extends ResultSetDataModelRow<M, C>, C>,
        C extends ResultSetDataModelCell<? extends ResultSetDataModelRow<M, C>, M>>
        extends SortableDataModelRow<M, C> {

    private int resultSetRowIndex;

    public ResultSetDataModelRow(M model, ResultSet resultSet, int resultSetRowIndex) throws SQLException {
        super(model);
        this.resultSetRowIndex = resultSetRowIndex;
        int columnCount = model.getColumnCount();
        List<C> cells = new CompactArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            ResultSetColumnInfo columnInfo = (ResultSetColumnInfo) getModel().getColumnInfo(i);
            C cell = createCell(resultSet, columnInfo);
            cells.set(i, cell);
        }
        this.setCells(cells);
    }

    @NotNull
    @Override
    public M getModel() {
        return super.getModel();
    }

    public int getResultSetRowIndex() {
        return resultSetRowIndex;
    }

    public void shiftResultSetRowIndex(int delta) {
        resultSetRowIndex = resultSetRowIndex + delta;
    }

    @NotNull
    protected C createCell(ResultSet resultSet, ColumnInfo columnInfo) throws SQLException {
        return (C) new ResultSetDataModelCell(this, resultSet, (ResultSetColumnInfo) columnInfo);
    }

    @Nullable
    @Override
    public C getCellAtIndex(int index) {
        return super.getCellAtIndex(index);
    }
}
