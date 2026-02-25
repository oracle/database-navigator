/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.vector.model.request;

import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.cache.DBObjectFilter;
import com.dbn.object.cache.DBObjectFilterType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.data.type.GenericDataType.CLOB;
import static com.dbn.data.type.GenericDataType.JSON;
import static com.dbn.data.type.GenericDataType.LITERAL;
import static com.dbn.data.type.GenericDataType.VECTOR;

public class EmbeddingDestinationTableFilter implements DBObjectFilter<DBTable> {

    @Override
    public boolean accepts(DBTable table) {
        List<DBColumn> columns = table.getColumns();
        if (columns.size() < 4) return false; // no exact match expected (consider system columns)

        Set<GenericDataType> columnTypes = columns.stream().map(c -> c.getDataType().getGenericDataType()).collect(Collectors.toSet());
        Set<GenericDataType> expectedColumnTypes = Set.of(LITERAL, CLOB, VECTOR, JSON);
        if (!columnTypes.containsAll(expectedColumnTypes)) return false;

        return true;
    }

    @Override
    public DBObjectFilterType getType() {
        return DBObjectFilterType.EMBEDDING_DESTINATION_TABLES;
    }
}
