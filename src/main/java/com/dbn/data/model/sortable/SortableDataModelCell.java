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

import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.basic.BasicDataModelCell;
import org.jetbrains.annotations.NotNull;

public class SortableDataModelCell<
        R extends SortableDataModelRow<M, ? extends SortableDataModelCell<R, M>>,
        M extends SortableDataModel<R, ? extends SortableDataModelCell<R, M>>>
        extends BasicDataModelCell<R, M>
        implements Comparable<DataModelCell> {

    public SortableDataModelCell(R row, Object userValue, int index) {
        super(userValue, row, index);
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

    @Override
    public int compareTo(@NotNull DataModelCell cell) {
        Object local = getUserValue();
        Object remote = cell.getUserValue();

        boolean nullsFirst = getModel().isSortingNullsFirst();

        if (local == null && remote == null) return 0;
        if (local == null) return nullsFirst ? -1 : 1;
        if (remote == null) return nullsFirst ? 1 : -1;
        // local class may differ from remote class for
        // columns with data conversion error
        Class<?> localClass = local.getClass();
        Class<?> remoteClass = remote.getClass();

        if (local instanceof Comparable && remote instanceof Comparable && localClass.equals(remoteClass)) {
            Comparable localComparable = (Comparable) local;
            Comparable remoteComparable = (Comparable) remote;
            return localComparable.compareTo(remoteComparable);
        } else {
            Class typeClass = cell.getColumnInfo().getDataType().getTypeClass();
            return localClass.equals(typeClass) ? 1 :
                   remoteClass.equals(typeClass) ? -1 : 0;
        }
    }
}
