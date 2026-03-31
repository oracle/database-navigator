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

package com.dbn.vfs.file;

import com.dbn.object.DBConsole;
import org.jetbrains.annotations.NotNull;

public class DBSearchConsoleVirtualFile extends DBConsoleVirtualFile {
    public static final String ATTR_VECTOR_SCHEMA = "search-schema";
    public static final String ATTR_VECTOR_TABLE = "search-table";
    public static final String ATTR_DISTANCE_METRIC = "distance-metric";

    public DBSearchConsoleVirtualFile(@NotNull DBConsole console) {
        super(console);
    }

    public String getSearchSchema() {
        return getAttribute(ATTR_VECTOR_SCHEMA);
    }

    public String getSearchTable() {
        return getAttribute(ATTR_VECTOR_TABLE);
    }

    public String getDistanceMetric() {
        return getAttribute(ATTR_DISTANCE_METRIC);
    }

    public void setSearchSchema(String vectorSchema) {
        setAttribute(ATTR_VECTOR_SCHEMA, vectorSchema);
    }

    public void setSearchTable(String vectorTable) {
        setAttribute(ATTR_VECTOR_TABLE, vectorTable);
    }

    public void setDistanceMetric(String distanceMetric) {
        setAttribute(ATTR_DISTANCE_METRIC, distanceMetric);
    }
}
