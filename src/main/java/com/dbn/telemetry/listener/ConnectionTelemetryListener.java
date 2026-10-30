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

import com.dbn.telemetry.TelemetryType;

import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dbn.common.outcome.OutcomeType.CANCELLED;
import static com.dbn.common.outcome.OutcomeType.ERROR;
import static com.dbn.common.outcome.OutcomeType.SUCCESS;
import static com.dbn.common.outcome.OutcomeType.TIMEOUT;

/** Measures managed connection establishment, including session initialization. */
@NonNls
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConnectionTelemetryListener extends DurationTelemetryListener {
    private static final ConnectionTelemetryListener INSTANCE = new ConnectionTelemetryListener();
    private static final List<Double> DURATION_BUCKETS_SECONDS = List.of(
            0.1, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0);

    @Override
    protected @NotNull TelemetryType getTelemetryType() {
        return TelemetryType.CONNECTION;
    }

    @Override
    protected @NotNull List<Double> getDurationBucketsSeconds() {
        return DURATION_BUCKETS_SECONDS;
    }

    public static <T> T collectTelemetryData(
            @NotNull ConnectionHandler connection,
            @NotNull ThrowableCallable<T, SQLException> callable) throws SQLException {
        if (!isCollectionEnabled()) return callable.call();

        String attemptType = connection.getConnectionInfo() == null ? "initial" : "subsequent";
        long startedAtNanos = start();
        OutcomeType outcome = ERROR;
        String failureCategory = "other";
        try {
            T result = callable.call();
            outcome = SUCCESS;
            failureCategory = "none";
            return result;
        } catch (ProcessCanceledException exception) {
            outcome = CANCELLED;
            failureCategory = "none";
            throw exception;
        } catch (SQLException exception) {
            failureCategory = classifyFailure(exception);
            if (failureCategory.equals("timeout")) outcome = TIMEOUT;
            throw exception;
        } finally {
            record(connection, startedAtNanos, attemptType, outcome, failureCategory);
        }
    }

    /** Uses exception types and standard SQLState classes, never exception text. */
    private static String classifyFailure(SQLException exception) {
        String category = "other";
        Throwable current = exception;
        // Bound traversal because JDBC drivers may expose cyclic exception chains.
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (current instanceof SQLTimeoutException || current instanceof SocketTimeoutException) return "timeout";
            if (current instanceof SQLInvalidAuthorizationSpecException) return "authentication";
            if (current instanceof SQLException sqlException) {
                String state = sqlException.getSQLState();
                if (state != null && state.startsWith("28")) return "authentication";
                if (state != null && state.startsWith("08")) category = "network";
                current = current.getCause() != null ? current.getCause() : sqlException.getNextException();
            } else {
                if (current instanceof java.net.SocketException || current instanceof java.net.UnknownHostException) category = "network";
                current = current.getCause();
            }
        }
        return category;
    }

    private static void record(
            ConnectionHandler connection,
            long startedAtNanos,
            String attemptType,
            OutcomeType outcome,
            String failureCategory) {
        INSTANCE.recordDuration(startedAtNanos, () -> {
            Map<String, String> attributes = new HashMap<>(databaseAttributes(() -> connection));
            attributes.put("attempt_type", attemptType);
            attributes.put("outcome", outcome.name().toLowerCase(Locale.ROOT));
            if (outcome == ERROR || outcome == TIMEOUT) attributes.put("failure_category", failureCategory);
            return attributes;
        });
    }
}
