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

package com.dbn.object;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DBTable extends DBDataset, com.dbn.api.object.DBTable {
    boolean isTemporary();
    @Override
    @Nullable
    List<DBIndex> getIndexes();
    List<DBNestedTable> getNestedTables();
    @Override
    @Nullable
    DBIndex getIndex(String name);
    DBNestedTable getNestedTable(String name);
    List<DBColumn> getPrimaryKeyColumns();
    List<DBColumn> getForeignKeyColumns();
    List<DBColumn> getUniqueKeyColumns();
}