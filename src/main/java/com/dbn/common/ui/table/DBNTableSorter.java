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

package com.dbn.common.ui.table;

import javax.swing.table.TableRowSorter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class DBNTableSorter<R, M extends DBNTableModel<R>> extends TableRowSorter<M> {
    private final Map<Integer, Comparator> comparators = new HashMap<>();

    public DBNTableSorter(M model) {
        super(model);
        setMaxSortKeys(1);
    }

    @Override
    public Comparator<?> getComparator(int column) {
        M model = getModel();
        return comparators.computeIfAbsent(column, c -> (Comparator<R>) (row1, row2) -> {
            Object value1 = model.getValue(row1, c);
            Object value2 = model.getValue(row2, c);

            if (value1 == null && value2 == null) {
                return 0;
            }

            if (value1 == null) {
                return -1;
            }

            if (value2 == null) {
                return 1;
            }

            if (value1 instanceof Comparable && value2 instanceof Comparable) {
                Comparable comparable1 = (Comparable) value1;
                Comparable comparable2 = (Comparable) value2;

                return comparable1.compareTo(comparable2);
            }

            String presentableValue1 = model.getPresentableValue(row1, c);
            String presentableValue2 = model.getPresentableValue(row2, c);
            return presentableValue1 == null ? -1 : presentableValue2 == null ? 1 : presentableValue1.compareTo(presentableValue2);
        });
    }

    @Override
    protected boolean useToString(int column) {
        return false;
    }


}
