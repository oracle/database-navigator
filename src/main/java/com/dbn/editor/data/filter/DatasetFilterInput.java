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

package com.dbn.editor.data.filter;

import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatasetFilterInput {
    private final DBObjectRef<DBDataset> dataset;
    private final Map<DBObjectRef<DBColumn>, Object> values = new LinkedHashMap<>();

    public DatasetFilterInput(DBDataset dataset) {
        this.dataset = DBObjectRef.of(dataset);
    }

    public DBDataset getDataset() {
        return dataset.get();
    }

    public List<DBColumn> getColumns() {
        return values.keySet().stream().map(ref -> ref.get()).collect(Collectors.toList());
    }

    public void setColumnValue(@NotNull DBColumn column, Object value) {
        values.put(DBObjectRef.of(column), value);
    }

    public Object getColumnValue(DBColumn column) {
        return values.get(DBObjectRef.of(column));
    }
}
