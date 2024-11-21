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

package com.dbn.database.common.util;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CachedResultSetRow {
    private final Map<String, Object> values = new HashMap<>();

    private CachedResultSetRow(@Nullable ResultSet source, List<String> columnNames) throws SQLException {
        if (source != null) {
            for (String columnName : columnNames) {
                Object columnValue = source.getObject(columnName);
                values.put(columnName, columnValue);
            }
        }
    }

    public static CachedResultSetRow create(ResultSet source, List<String> columnNames) throws SQLException {
        return new CachedResultSetRow(source, columnNames);
    }

    public Object get(String columnName) {
        return values.get(columnName);
    }


    public boolean matches(CachedResultSetRow that, CachedResultSet.Columns columns) {
        for (String columnName : columns.names()) {
            Object thisColumnValue = this.get(columnName);
            Object thatColumnValue = that.get(columnName);
            if (!Commons.match(thisColumnValue, thatColumnValue)) {
                return false;
            }
        }
        return true;
    }

    public CachedResultSetRow clone(CachedResultSet.Columns columns) throws SQLException {
        CachedResultSetRow clone = new CachedResultSetRow(null, null);
        String[] columnNames = columns.names();
        for (String columnName : values.keySet()) {
            if (Strings.isOneOf(columnName, columnNames)) {
                Object columnValue = get(columnName);
                clone.values.put(columnName, columnValue);
            }
        }
        return clone;
    }

    void extend(String columnName, Object columnValue){
        values.put(columnName, columnValue);
    }

    void normalize(CachedResultSet.Mapper<String> columnMapper) {
        for (String columnName : new HashSet<>(values.keySet())) {
            String newColumnName = columnMapper.map(columnName);
            if (newColumnName != null && !Objects.equals(newColumnName, columnName)) {
                Object columnValue = values.remove(columnName);
                values.put(newColumnName, columnValue);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Object column : values.values()) {
            if (builder.length() > 0) builder.append(" / ");
            builder.append(column);
        }

        return builder.toString();
    }
}
