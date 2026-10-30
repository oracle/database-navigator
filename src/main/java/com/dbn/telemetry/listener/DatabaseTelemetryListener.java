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

package com.dbn.telemetry.listener;

import com.dbn.common.routine.ThrowableCallable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregates database interface execution; result-set consumption is outside this measurement. */
@NonNls
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DatabaseTelemetryListener extends TelemetryListener {
    private static final DatabaseTelemetryListener INSTANCE = new DatabaseTelemetryListener();
    private static final String DURATION_METRIC_NAME = "dbn.database.interface.duration";
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0);

    @Override
    protected @NotNull String getMetricName() {
        return DURATION_METRIC_NAME;
    }

    @Override
    protected @NotNull List<Double> getDurationBucketsSeconds() {
        return DURATION_BUCKETS_SECONDS;
    }

    public static <T> T collectTelemetryData(
            @NotNull DBNConnection connection,
            @NotNull String statementId,
            @NotNull ThrowableCallable<T, SQLException> callable) throws SQLException {
        long startedAtNanos = start();

        @NonNls
        String outcome = "error";
        try {
            T result = callable.call();
            outcome = "success";
            return result;
        } catch (SQLTimeoutException exception) {
            outcome = "timeout";
            throw exception;
        } catch (ProcessCanceledException exception) {
            outcome = "cancelled";
            throw exception;
        } finally {
            record(connection, statementId, startedAtNanos, outcome);
        }
    }

    private static void record(
            @NotNull DBNConnection connection,
            @NotNull String statementId,
            long startedAtNanos,
            @NotNull String outcome) {
        INSTANCE.recordDuration(startedAtNanos, () -> {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("statement_id", statementId);
            attributes.put("database_type", connection.getDatabaseType().name());
            attributes.put("outcome", outcome);

            ConnectionHandler handler = connection.getConnectionHandler();
            if (handler != null) {
                double version = handler.getDatabaseVersion();
                // Connection settings use 9999 until the database version is known.
                if (Double.isFinite(version) && version > 0 && version != 9999) {
                    attributes.put("database_version", Double.toString(version));
                }
            }
            return attributes;
        });
    }
}
