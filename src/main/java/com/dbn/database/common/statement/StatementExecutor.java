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

package com.dbn.database.common.statement;

import com.dbn.common.thread.Threads;
import com.dbn.common.thread.Timeout;
import com.dbn.connection.Resources;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.dbn.common.exception.Exceptions.causeOf;
import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.exception.Exceptions.toSqlTimeoutException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@UtilityClass
public final class StatementExecutor {

    public static <T> T execute(StatementExecutorContext context, Callable<T> callable) throws SQLException {
        long start = System.currentTimeMillis();
        int timeout = context.getTimeout();
        try {
            ExecutorService executorService = Threads.databaseInterfaceExecutor();
            Future<T> future = executorService.submit(callable);
            T result = Timeout.waitFor(future, timeout, TimeUnit.SECONDS);

            context.log("QUERY", false, false, millisSince(start));
            return result;

        } catch (TimeoutException | InterruptedException | RejectedExecutionException e) {
            conditionallyLog(e);
            context.log("QUERY", false, true, millisSince(start));
            Resources.close(context.getStatement());
            throw toSqlTimeoutException(e, "Operation timed out (timeout = " + timeout + "s)");

        } catch (ExecutionException e) {
            conditionallyLog(e);
            context.log("QUERY", true, false, millisSince(start));
            Resources.close(context.getStatement());
            Throwable cause = causeOf(e);
            throw toSqlException(cause, "Error processing request: " + cause.getMessage());

        } catch (Throwable e) {
            conditionallyLog(e);
            context.log("QUERY", true, false, millisSince(start));
            Resources.close(context.getStatement());
            throw toSqlException(e, "Error processing request: " + e.getMessage());

        }
    }

    private static long millisSince(long start) {
        return System.currentTimeMillis() - start;
    }
}
