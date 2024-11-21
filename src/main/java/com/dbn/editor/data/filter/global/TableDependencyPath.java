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

package com.dbn.editor.data.filter.global;

import com.dbn.object.DBTable;

import java.util.ArrayList;
import java.util.List;

public class TableDependencyPath {
    private final List<DBTable> tables = new ArrayList<>();

    @Override
    public TableDependencyPath clone() {
        TableDependencyPath clone = new TableDependencyPath();
        clone.tables.addAll(tables);
        return clone;
    }

    public void addTable(DBTable table) {
        tables.add(table);
    }

   public boolean isRecursive(DBTable table) {
        return tables.contains(table);
    }

    public static TableDependencyPath buildDependencyPath(TableDependencyPath path, DBTable sourceTable, DBTable targetTable) {
        if (path == null) {
            path = new TableDependencyPath();
        } else {
            if (path.isRecursive(sourceTable)) {
                return null;
            } else {
                path = path.clone();
            }
        }

        path.addTable(sourceTable);
        return null;


    }

}
