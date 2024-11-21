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

import com.dbn.common.data.Data;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.util.Lists;
import com.dbn.connection.Resources;
import com.dbn.connection.ResultSets;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public class CachedResultSet extends StatefulDisposableBase implements ResultSetStub {
    private List<CachedResultSetRow> rows = new ArrayList<>();
    private List<String> columnNames;

    public static final CachedResultSet EMPTY = new CachedResultSet(
            Collections.emptyList(),
            Collections.emptyList()){
        @Override
        public CachedResultSet open() {
            return this;
        }

        @Override
        public boolean next() throws SQLException {
            return false;
        }
    };

    private final Map<Condition, CachedResultSet> filtered = new ConcurrentHashMap<>();

    private final Map<Columns, CachedResultSet> grouped = new ConcurrentHashMap<>();

    private CachedResultSet(@Nullable ResultSet source, @Nullable ResultSetCondition condition) throws SQLException {
        if (source instanceof CachedResultSet) {
            CachedResultSet cachedResultSet = (CachedResultSet) source;
            this.columnNames = new ArrayList<>(cachedResultSet.columnNames);
            source = cachedResultSet.open();
            load(source, condition);
        } else if (source != null && !Resources.isClosed(source)) {
            try {
                List<String> columnNames = ResultSets.getColumnNames(source);
                this.columnNames = Lists.convert(columnNames, s -> cachedUpperCase(s.trim()));
                load(source, condition);
            } finally {
                Resources.close(source);
            }
        }
    }

    private CachedResultSet(@NotNull CachedResultSet source, @NotNull Condition condition) throws SQLException {
        columnNames = new ArrayList<>(source.columnNames);
        for (CachedResultSetRow row : source.rows) {
            if (condition.evaluate(row)) {
                rows.add(row);
            }
        }
    }

    /**
     * Internal constructor for clone on iteration
     */
    private CachedResultSet(List<CachedResultSetRow> rows, List<String> columnNames) {
        this.rows = rows;
        this.columnNames = columnNames;
    }

    /**
     * Load and cache the result set passed in as parameter
     * @param resultSet the result set to cache
     * @param condition the condition to filter out records from original result set
     * @throws SQLException propagated from original result set evaluations
     */
    private void load(@NotNull ResultSet resultSet, @Nullable ResultSetCondition condition) throws SQLException {
        ResultSets.forEachRow(resultSet, () -> {
            if (condition == null || condition.evaluate(resultSet)) {
                CachedResultSetRow row = CachedResultSetRow.create(resultSet, columnNames);
                rows.add(row);
            }
        });
    }

    public static <T> CachedResultSet create(@Nullable ResultSet resultSet) throws SQLException {
        return create(resultSet, null);
    }

    public static <T> CachedResultSet create(@Nullable ResultSet resultSet, @Nullable ResultSetCondition condition) throws SQLException {
        return new CachedResultSet(resultSet, condition);
    }

    public List<CachedResultSetRow> rows() {
        return rows;
    }

    public CachedResultSetRow rowAt(int index) {
        return rows.get(index);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
    /**
     * Current row at cursor
     */
    protected CachedResultSetRow current() {
        throw new IllegalStateException("Call open() on CachedResultSet to initialize isolated iteration (avoid concurrency issues)");
    }

    @Override
    public void close() {
        // source already closed;
    }

    @Override
    public boolean isClosed() {
        return false; // cached - never closed
    }

    /**************************************************************
     *                          Utilities                         *
     **************************************************************/
    public CachedResultSet where(@NotNull Condition whereCondition) {
        return filtered.computeIfAbsent(whereCondition, c -> createFilteredSet(c));
    }

    @NotNull
    private CachedResultSet createFilteredSet(Condition condition) {
        if (rows.isEmpty()) {
            return this;
        } else {
            List<CachedResultSetRow> filteredRows = new ArrayList<>();
            for (CachedResultSetRow element : rows) {
                try {
                    if (condition.evaluate(element)) {
                        filteredRows.add(element);
                    }
                } catch (SQLException e) {
                    conditionallyLog(e);
                    log.error("Failed to filter cached result set", e);
                }
            }
            return new CachedResultSet(filteredRows, columnNames);
        }
    }

    public CachedResultSet filter(@NotNull Condition whereCondition) throws SQLException {
        return new CachedResultSet(this, whereCondition);
    }

    public CachedResultSet filter(@NotNull ResultSetCondition whereCondition) throws SQLException {
        return new CachedResultSet(this, whereCondition);
    }

    public CachedResultSet normalize(Mapper<String> columnMapper) {
        columnNames = Lists.convert(columnNames, columnName -> nvl(columnMapper.map(columnName), columnName));
        for (CachedResultSetRow row : rows) {
            row.normalize(columnMapper);
        }
        return this;
    }

    public CachedResultSet enrich(String columnName, ColumnValue value) throws SQLException {
        columnNames.add(columnName);
        for (int i = 0; i < rows.size(); i++) {
            CachedResultSetRow row = rows.get(i);
            row.extend(columnName, value.resolve(this, i));
        }
        return this;
    }

    @Nullable
    public CachedResultSetRow first(@NotNull Condition whereCondition) throws SQLException {
        return next(whereCondition, 0);
    }

    /**
     * Next row from index (inclusive) matching the condition
     */
    public CachedResultSetRow next(@NotNull Condition whereCondition, int fromIndex) throws SQLException {
        for (int i = fromIndex; i < rows.size(); i++) {
            CachedResultSetRow row = rows.get(i);
            if (whereCondition.evaluate(row)) {
                return row;
            }
        }
        return null;
    }

    /**
     * Previous row from index (non-inclusive) matching the condition
     */
    public CachedResultSetRow previous(@NotNull Condition whereCondition, int fromIndex) throws SQLException {
        for (int i = fromIndex-1; i >= 0; i--) {
            CachedResultSetRow row = rows.get(i);
            if (whereCondition.evaluate(row)) {
                return row;
            }
        }
        return null;
    }

    public int count(@NotNull Condition whereCondition) throws SQLException {
        int count = 0;
        for (int i = 0; i < rows.size(); i++) {
            CachedResultSetRow row = rows.get(i);
            if (whereCondition.evaluate(row)) {
                count++;
            }
        }
        return count;
    }

    public boolean exists(@NotNull Condition whereCondition) throws SQLException {
        return first(whereCondition) != null;
    }

    @NotNull
    public <T> List<T> list(String columnName, Class<T> columnType) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CachedResultSetRow row = rows.get(i);
            Object columnValue = row.get(columnName);
            T castedValue = Data.cast(columnValue, columnType);
            list.add(castedValue);
        }

        return list;
    }

    @NotNull
    private CachedResultSet open(List<CachedResultSetRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return EMPTY;
        } else {
            return new CachedResultSet(rows, columnNames) {
                private int cursor = -1;

                @Override
                public boolean next() {
                    if (cursor == rows.size()) {
                        throw new IllegalStateException("Result set exhausted. Call open() again");
                    }
                    cursor++;
                    return cursor < rows.size();
                }

                @Override
                protected CachedResultSetRow current() {
                    return rows.get(cursor);
                }
            };
        }
    }

    public CachedResultSet unionAll(CachedResultSet resultSet) {
        if (isEmpty()) {
            return resultSet;

        } else if (resultSet.isEmpty()) {
            return this;

        } else {
            List<CachedResultSetRow> newRows = new ArrayList<>(rows);
            newRows.addAll(resultSet.rows);
            return new CachedResultSet(newRows, columnNames);
        }
    }

    /**
     * Cached result sets are accessed concurrently
     * Open a copy with it's own cursor index to iterate
     */
    public CachedResultSet open() {
        return open(rows);
    }

    public CachedResultSet groupBy(@NotNull Columns groupByClause) throws SQLException {
        if (rows.isEmpty()) {
            return this;
        } else {
            return grouped.computeIfAbsent(groupByClause, c -> createGroupBySet(c));
        }
    }

    @NotNull
    private CachedResultSet createGroupBySet(Columns columns) {
        if (rows.isEmpty()) {
            return this;
        } else {
            List<CachedResultSetRow> groupedRows = new ArrayList<>();
            try {

                for (CachedResultSetRow row : rows) {
                    CachedResultSetRow matchingRow = find(groupedRows, row, columns);
                    if (matchingRow == null) {
                        groupedRows.add(row.clone(columns));
                    } else {
                        // TODO ignore or pivot the rest of columns?
                    }
                }
            } catch (Throwable e) {
                conditionallyLog(e);
                log.error("Failed to group cached result set", e);
            }

            return new CachedResultSet(groupedRows, columnNames);
        }
    }



    private static CachedResultSetRow find(List<CachedResultSetRow> list, CachedResultSetRow match, Columns columns) {
        for (int i = 0; i < list.size(); i++) {
            CachedResultSetRow row = list.get(i);
            if (row.matches(match, columns)) {
                return row;
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface Condition {
        boolean evaluate(CachedResultSetRow row) throws SQLException;

        static Condition in(CachedResultSet source, Columns matchColumns) {
            return row -> source.exists(sourceRow ->
                    sourceRow.matches(row, matchColumns));
        }

        static Condition notIn(CachedResultSet source, Columns matchColumns) {
            return row -> !source.exists(sourceRow ->
                    sourceRow.matches(row, matchColumns));
        }
    }

    @FunctionalInterface
    public interface ColumnValue {
        Object resolve(CachedResultSet resultSet, int index) throws SQLException;
    }

    @FunctionalInterface
    public interface Columns {
        String[] names();
    }

    @FunctionalInterface
    public interface Mapper<T> {
        T map(T original);
    }


    /**************************************************************
     *            Value accessor implementations                  *
     **************************************************************/
    @Override
    public Object getObject(String columnLabel) throws SQLException {
        CachedResultSetRow currentRow = current();
        return currentRow.get(columnLabel);
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return Data.asString(value);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return Data.asShrt(value);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return Data.asInt(value);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return Data.asLng(value);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        return Data.asBool(value);
    }

    // TODO add more accessors overrides if needed


    @Override
    public void disposeInner() {
        nullify();
    }
}
