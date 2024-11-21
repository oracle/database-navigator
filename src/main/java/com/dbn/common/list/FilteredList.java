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

package com.dbn.common.list;

import com.dbn.common.filter.Filter;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public interface FilteredList<T> extends List<T> {
    List<T> getBase();

    @Nullable
    Filter<T> getFilter();

    // update methods should not be affected by filtering
    @Override
    void sort(Comparator<? super T> comparator);

    void trimToSize();

    void setFilter(Filter<T> filter);

    static <T> StatefulFilteredList<T> stateful(Filter<T> filter) {
        return new StatefulFilteredList<>(filter);
    }

    static <T> StatefulFilteredList<T> stateful(Filter<T> filter, List<T> base) {
        return new StatefulFilteredList<>(filter, base);
    }

    static <T> StatelessFilteredList<T> stateless(Filter<T> filter) {
        return new StatelessFilteredList<>(filter);
    }

    static <T> StatelessFilteredList<T> stateless(Filter<T> filter, List<T> base) {
        return new StatelessFilteredList<>(filter, base);
    }

    static <T> List<T> unwrap(List<T> list) {
        if (list instanceof FilteredList) {
            FilteredList<T> filteredList = (FilteredList<T>) list;
            return filteredList.getBase();
        }
        return list;
    }
}
