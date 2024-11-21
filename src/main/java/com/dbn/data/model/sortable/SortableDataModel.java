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

package com.dbn.data.model.sortable;

import com.dbn.common.list.FilteredList;
import com.dbn.common.list.StatelessFilteredList;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.grid.options.DataGridSortingSettings;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.DataModelState;
import com.dbn.data.model.basic.BasicDataModel;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.sorting.SortingState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;


public class SortableDataModel<
        R extends SortableDataModelRow<? extends SortableDataModel<R, C>, C>,
        C extends SortableDataModelCell<R, ? extends SortableDataModel<R, C>>>
        extends BasicDataModel<R, C> {

    private boolean sortingNullsFirst;

    protected SortableDataModel(Project project) {
        super(project);
    }

    public boolean sort(int columnIndex, SortDirection direction, boolean keepExisting) {
        boolean sort = updateSortingState(columnIndex, direction, keepExisting);
        if (sort) {
            sort();
            notifyRowsUpdated(0, getRows().size());
        }
        return sort;
    }

    @NotNull
    @Override
    public SortableDataModelState getState() {
        return (SortableDataModelState) super.getState();
    }

    @Override
    protected DataModelState createState() {
        return new SortableDataModelState();
    }

    private boolean updateSortingState(int columnIndex, SortDirection direction, boolean keepExisting) {
        SortingState sortingState = getSortingState();
        String columnName = getColumnName(columnIndex);
        int maxSortingColumns = getSortingSettings().getMaxSortingColumns();
        return sortingState.applySorting(columnName, direction, keepExisting, maxSortingColumns);
    }

    private DataGridSortingSettings getSortingSettings() {
        DataGridSettings dataGridSettings = DataGridSettings.getInstance(getProject());
        return dataGridSettings.getSortingSettings();
    }


    protected void sortByIndex() {
        getRows().sort(INDEX_COMPARATOR);
        updateRowIndexes(0);
    }

    public void sort() {
        sort(getRows());
    }

    protected void sort(List<R> rows) {
        if (rows instanceof StatelessFilteredList) {
            FilteredList<R> filteredList = (FilteredList<R>) rows;
            rows = filteredList.getBase();
        }
        if (getSortingState().isValid()) {
            this.sortingNullsFirst = DataGridSettings.getInstance(getProject()).getSortingSettings().isNullsFirst();
            rows.sort(null);
        }
        updateRowIndexes(rows, 0);
    }

    private static final Comparator<DataModelRow> INDEX_COMPARATOR = Comparator.comparingInt(DataModelRow::getIndex);


/*
    public SortDirection getSortDirection() {
        return getSortingState().getDirection();
    }
*/

    public SortingState getSortingState() {
        return getState().getSortingState();
    }

    public void setSortingNullsFirst(boolean sortingNullsFirst) {
        this.sortingNullsFirst = sortingNullsFirst;
    }

    public boolean isSortingNullsFirst() {
        return sortingNullsFirst;
    }

/*
    public int getSortColumnIndex() {
        // fixme - cache sort column index somehow
        SortableDataModelState modelState = getState();
        return isDisposed() || modelState == null ? -1 :
                getHeader().getColumnIndex(modelState.getSortingState().getColumnName());
    }
*/
}
