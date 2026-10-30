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

import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.execution.ExecutionType;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/** Mutable telemetry state for one execution, separate from its cached listener. */
public final class ExecutionTelemetryWrapper {
    private volatile OutcomeType outcome;
    private volatile boolean cancellationRequested;

    private final ExecutionTelemetryListener listener;

    private ExecutionTelemetryWrapper(@NotNull ExecutionTelemetryListener listener) {
        this.listener = listener;
    }

    public static @NotNull ExecutionTelemetryWrapper telemetryWrapper(@NotNull ExecutionType executionType) {
        ExecutionTelemetryListener listener = ExecutionTelemetryListener.getInstance(executionType);
        return new ExecutionTelemetryWrapper(listener);
    }

    public <T> T collectTelemetryData(@NotNull ThrowableCallable<T, SQLException> callable) throws SQLException {
        long startedAtNanos = TelemetryListener.start();
        try {
            T result = callable.call();
            if (outcome == null) outcome = OutcomeType.SUCCESS;
            return result;
        } catch (SQLTimeoutException exception) {
            outcome = OutcomeType.TIMEOUT;
            throw exception;
        } catch (ProcessCanceledException exception) {
            outcome = OutcomeType.CANCELLED;
            throw exception;
        } catch (SQLException exception) {
            outcome = cancellationRequested ? OutcomeType.CANCELLED : OutcomeType.ERROR;
            throw exception;
        } catch (RuntimeException | Error exception) {
            outcome = OutcomeType.ERROR;
            throw exception;
        } finally {
            listener.recordExecution(startedAtNanos, outcome == null ? OutcomeType.ERROR : outcome);
        }
    }

    /** For execution paths that handle failures internally instead of propagating them. */
    public synchronized void setOutcome(@NotNull OutcomeType outcome) {
        if (this.outcome != OutcomeType.TIMEOUT) this.outcome = outcome;
    }

    public void collectCancellationData(@NotNull Runnable cancellation) {
        cancellationRequested = true;
        long startedAtNanos = TelemetryListener.start();
        boolean completed = false;
        try {
            cancellation.run();
            completed = true;
        } finally {
            OutcomeType cancellationOutcome = completed ? OutcomeType.SUCCESS : OutcomeType.ERROR;
            listener.recordCancellation(startedAtNanos, cancellationOutcome);
        }
    }
}
