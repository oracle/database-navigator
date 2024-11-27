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

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.data.model.sortable.SortableDataModelCell;
import com.dbn.data.type.DBDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.editor.data.model.RecordStatus.INSERTING;

public class ResultSetDataModelCell<
        R extends ResultSetDataModelRow<M, ? extends ResultSetDataModelCell<R, M>>,
        M extends ResultSetDataModel<R, ? extends ResultSetDataModelCell<R, M>>>
        extends SortableDataModelCell<R, M> {

    public ResultSetDataModelCell(R row, ResultSet resultSet, ResultSetColumnInfo columnInfo) throws SQLException {
        super(row, null, columnInfo.getIndex());
        DBDataType dataType = columnInfo.getDataType();
        if (!getModel().is(INSERTING)) {
            Object userValue = dataType.getValueFromResultSet(resultSet, columnInfo.getResultSetIndex());
            setUserValue(userValue);
        }
    }

    @NotNull
    @Override
    public M getModel() {
        return super.getModel();
    }

    @NotNull
    @Override
    public R getRow() {
        return super.getRow();
    }

    @NotNull
    protected DBNConnection getResultConnection() {
        return getRow().getModel().getResultConnection();
    }
}
