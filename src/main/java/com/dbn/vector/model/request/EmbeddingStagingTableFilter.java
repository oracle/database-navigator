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

import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.cache.DBObjectFilter;
import com.dbn.object.cache.DBObjectFilterType;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EmbeddingStagingTableFilter implements DBObjectFilter<DBTable> {

    @Override
    public boolean accepts(DBTable table) {
        // TODO create generic DBObjectFactoryInput.matchesObject();

        List<DBColumn> columns = table.getColumns();
        if (columns.size() < 5) return false; // no exact match expected (consider system columns)

        @NonNls
        Set<String> expectedColumnNames = Set.of("ID", "FILE_SIZE", "FILE_HASH", "FILE_CONTENT", "METADATA");
        Set<String> columnNames = columns.stream().map(c -> c.getName()).collect(Collectors.toSet());
        if (!columnNames.containsAll(expectedColumnNames)) return false;

        return true;
    }

    @Override
    public DBObjectFilterType getType() {
        return DBObjectFilterType.EMBEDDING_STAGING_TABLES;
    }
}
