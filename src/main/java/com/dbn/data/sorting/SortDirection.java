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

package com.dbn.data.sorting;

public enum SortDirection {
    INDEFINITE(0, ""),
    ASCENDING(1, "asc"),
    DESCENDING(-1, "desc");

    private final int compareAdj;
    private final String sqlToken;

    SortDirection(int compareAdj, String sqlToken) {
        this.compareAdj = compareAdj;
        this.sqlToken = sqlToken;
    }

    public int getCompareAdj() {
        return compareAdj;
    }

    public boolean isIndefinite() {
        return this == INDEFINITE;
    }

    public String getSqlToken() {
        return sqlToken;
    }
}
