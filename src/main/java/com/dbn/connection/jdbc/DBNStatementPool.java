/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.connection.jdbc;

import com.dbn.common.util.Unsafe;
import com.dbn.connection.Resources;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A small per-connection pool for prepared and callable statements.
 *
 * <p>JDBC statements are mutable and cannot be shared by concurrent callers.
 * A statement is therefore borrowed exclusively and returned with {@link #release(DBNPreparedStatement)}
 * after the execution (or its result set) has completed. Closing a statement removes it from the pool.
 */
public final class DBNStatementPool {
    private static final int MAX_IDLE_STATEMENTS_PER_KEY = 4;

    private final Map<StatementKey, StatementBucket> buckets = new ConcurrentHashMap<>();
    private final Map<DBNPreparedStatement<?>, StatementBucket> statementBuckets = new ConcurrentHashMap<>();
    private final Set<DBNPreparedStatement<?>> statements = ConcurrentHashMap.newKeySet();
    private final Set<DBNPreparedStatement<?>> borrowedStatements = ConcurrentHashMap.newKeySet();
    private final DBNConnection connection;
    private volatile boolean closed;

    public DBNStatementPool(@NotNull DBNConnection connection) {
        this.connection = connection;
    }

    public <S extends DBNPreparedStatement<?>> S borrow(
            @NotNull String sql,
            @NotNull Class<S> statementType,
            @NotNull Supplier<S> factory) {
        if (closed) {
            throw new IllegalStateException("Statement pool is closed");
        }

        StatementKey key = new StatementKey(sql, statementType);
        StatementBucket bucket = buckets.computeIfAbsent(key, k -> new StatementBucket());
        DBNPreparedStatement<?> statement = null;
        synchronized (bucket) {
            while (!bucket.idleStatements.isEmpty()) {
                DBNPreparedStatement<?> candidate = bucket.idleStatements.removeFirst();
                if (statementType.isInstance(candidate) && !candidate.isClosed()) {
                    statement = candidate;
                    break;
                }
                discard(candidate);
            }

            if (statement == null) {
                statement = factory.get();
                statement.setCached(true);
                statement.setFetchSize(500);
                statement.setSql(sql);
                statements.add(statement);
                statementBuckets.put(statement, bucket);
            }

            borrowedStatements.add(statement);
        }

        connection.activate(statement);
        return Unsafe.cast(statement);
    }

    public void release(@NotNull DBNPreparedStatement<?> statement) {
        if (!borrowedStatements.remove(statement)) return;

        StatementBucket bucket = statementBuckets.get(statement);
        if (closed || bucket == null || statement.isClosed()) {
            discard(statement);
            close(statement);
            return;
        }

        try {
            statement.clearParameters();
            statement.clearWarnings();
        } catch (SQLException e) {
            discard(statement);
            close(statement);
            return;
        }

        boolean discard = false;
        synchronized (bucket) {
            if (closed || bucket.idleStatements.size() >= MAX_IDLE_STATEMENTS_PER_KEY) {
                discard = true;
            } else {
                bucket.idleStatements.addLast(statement);
            }
        }

        if (discard) {
            discard(statement);
            close(statement);
        }
    }

    /**
     * Removes a statement from the pool after it has been closed or when an execution failed.
     * This method intentionally does not close the statement itself.
     */
    public void discard(@NotNull DBNPreparedStatement<?> statement) {
        borrowedStatements.remove(statement);
        StatementBucket bucket = statementBuckets.remove(statement);
        statements.remove(statement);
        if (bucket == null) return;

        synchronized (bucket) {
            bucket.idleStatements.remove(statement);
        }
    }

    public int size() {
        return statements.size();
    }

    public void close() {
        closed = true;
        List<DBNPreparedStatement<?>> snapshot = new ArrayList<>(statements);
        buckets.clear();
        statementBuckets.clear();
        statements.clear();
        borrowedStatements.clear();
        for (DBNPreparedStatement<?> statement : snapshot) {
            close(statement);
        }
    }

    private static void close(DBNPreparedStatement<?> statement) {
        Resources.close(statement);
    }

    private static final class StatementBucket {
        private final Deque<DBNPreparedStatement<?>> idleStatements = new ArrayDeque<>();
    }

    private record StatementKey(String sql, Class<?> statementType) {
    }
}
