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

import java.util.List;

public abstract class FilteredListBase<T> implements FilteredList<T> {
    protected List<T> base;
    protected Filter<T> filter;


    public FilteredListBase(Filter<T> filter, List<T> base) {
        this.base = initBase(base);
        this.filter = filter;
    }

    public FilteredListBase(Filter<T> filter) {
        this.base = initBase(null);
        this.filter = filter;
    }

    abstract List<T> initBase(List<T> source);

    @Nullable
    @Override
    public final Filter<T> getFilter() {
        return filter;
    }

    public void setFilter(Filter<T> filter) {
        this.filter = filter;
    }
}
