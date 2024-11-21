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

import com.dbn.data.model.basic.BasicDataModelRow;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.data.sorting.SortingState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SortableDataModelRow<
        M extends SortableDataModel<? extends SortableDataModelRow<M, C>, C>,
        C extends SortableDataModelCell<? extends SortableDataModelRow<M, C>, M>>
        extends BasicDataModelRow<M, C>
        implements Comparable {

    protected SortableDataModelRow(M model) {
        super(model);
    }

    @NotNull
    @Override
    public M getModel() {
        return super.getModel();
    }

    @Nullable
    @Override
    public C getCellAtIndex(int index) {
        return super.getCellAtIndex(index);
    }

    @Override
    public int compareTo(@NotNull Object o) {
        SortableDataModelRow row = (SortableDataModelRow) o;
        SortableDataModel model = getModel();
        SortingState sortingState = model.getSortingState();

        for (SortingInstruction sortingInstruction : sortingState.getInstructions()) {
            int columnIndex = model.getColumnIndex(sortingInstruction.getColumnName());

            if (columnIndex > -1) {
                SortableDataModelCell local = getCellAtIndex(columnIndex);
                SortableDataModelCell remote = row.getCellAtIndex(columnIndex);

                int compareIndex = sortingInstruction.getDirection().getCompareAdj();

                int result =
                        remote == null && local == null ? 0 :
                        local == null ? -compareIndex :
                        remote == null ? columnIndex :
                                compareIndex * local.compareTo(remote);

                if (result != 0) return result;
            }
        }
        return 0;


/*
        int index = model.getSortColumnIndex();

        if (index == -1) return 0;
        SortableDataModelRow row = (SortableDataModelRow) o;

        SortableDataModelCell local = getCellAtIndex(index);
        SortableDataModelCell remote = row.getCellAtIndex(index);

        int compareIndex = model.getSortDirection().getCompareIndex();

        if (remote == null && local == null) return 0;
        if (local == null) return -compareIndex;
        if (remote == null) return compareIndex;

        return compareIndex * local.compareTo(remote);
*/
    }

}
