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

package com.dbn.data.model;

import com.dbn.common.filter.Filter;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.data.find.DataSearchResult;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DataModel<
        R extends DataModelRow<? extends DataModel<R, C>, C>,
        C extends DataModelCell<R, ? extends DataModel<R, C>>>
        extends DBNTableWithGutterModel {

    boolean isReadonly();

    @NotNull
    Project getProject();

    void setFilter(Filter<R> filter);

    @Nullable
    Filter<R> getFilter();

    @NotNull
    List<R> getRows();

    int indexOfRow(R row);

    @Nullable
    R getRowAtIndex(int index);

    DataModelHeader getHeader();

    ColumnInfo getColumnInfo(int columnIndex);

    @NotNull
    DataModelState getState();

    void setState(DataModelState state);

    DataSearchResult getSearchResult();

    void addDataModelListener(DataModelListener listener);

    void removeDataModelListener(DataModelListener listener);

    boolean hasSearchResult();

    int getColumnIndex(String columnName);
}
