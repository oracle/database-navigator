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

package com.dbn.common.exception;

import com.dbn.common.lookup.Visitor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

public class Exceptions {
    public static final SQLNonTransientConnectionException DBN_NOT_CONNECTED_EXCEPTION =
            new SQLNonTransientConnectionException(txt("msg.connection.exception.NotConnectedToDatabase"));

    private Exceptions() {}

    @NotNull
    public static SQLException toSqlException(@NotNull Throwable e) {
        if (e instanceof SQLException) return (SQLException) e;

        e = unwrap(e);
        return new SQLException(throwableMessage(e), e);
    }

    @NotNull
    public static SQLException toSqlException(@NotNull Throwable e, @Nls String s) {
        if (e instanceof SQLException) return (SQLException) e;

        e = unwrap(e);
        String reason = normalizeMessage(e, s);
        return new SQLException(reason, e);
    }

    @NotNull
    public static SQLTimeoutException toSqlTimeoutException(@NotNull Throwable e, @Nls String s) {
        if (e instanceof SQLTimeoutException) return (SQLTimeoutException) e;
        String reason = normalizeMessage(e, s);
        return new SQLTimeoutException(reason, e);
    }

    private static @NotNull String normalizeMessage(@NotNull Throwable e, @Nls String s) {
        // remove duplicate message content for nested exceptions propagating own message
        String message = nvl(e.getMessage(), "");
        s = s.replace(message, "");
        return s + "[" + simpleClassName(e) + "] " + message;
    }

    @NotNull
    public static RuntimeException toRuntimeException(@NotNull Throwable e) {
        if (e instanceof RuntimeException) return (RuntimeException) e;
        return new RuntimeException(throwableMessage(e), e);
    }

    @NotNull
    private static String throwableMessage(@NotNull Throwable e) {
        return nvl(e.getMessage(), simpleClassName(e));
    }

    public static <T> T unsupported() {
        throw new UnsupportedOperationException();
    }

    public static <T, E extends Enum> T unsupported(E enumeration) {
        throw new UnsupportedOperationException(
                txt("msg.shared.exception.UnsupportedEnumeration", simpleClassName(enumeration), enumeration));
    }


    public static TimeoutException timeoutException(long time, TimeUnit timeUnit) {
        return new TimeoutException(
                txt("msg.shared.exception.OperationTimedOut", presentableDuration(timeUnit.toMillis(time), false)));
    }

    public static Throwable causeOf(Throwable e) {
        return nvl(e.getCause(), e);
    }

    public static Throwable rootCauseOf(Throwable e) {
        while (e != null && e.getCause() != null && e.getCause() != e) {
            e = e.getCause();
        }
        return e;
    }

    public static String causeMessage(Throwable e) {
        return causeOf(e).getMessage();
    }

    public static void illegalState(@NonNls String message) {
        throw new IllegalStateException(message);
    }

    /**
     * Visit the cause chaing of t using visitor
     * @param visitor
     * @param t
     */
    public static void accept(Visitor<Throwable> visitor, Throwable t) {
        Set<Throwable> visited = new HashSet<>();
        while (t != null && !visited.contains(t)) {
            visited.add(t);
            visitor.visit(t);
            t = t.getCause();
        }
    }

    public static String getMessage(Throwable e) {
        return nvl(e.getMessage(), simpleClassName(e));
    }

    public static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof UndeclaredThrowableException undeclaredThrowableException) {
            Throwable undeclaredThrowable = undeclaredThrowableException.getUndeclaredThrowable();
            return undeclaredThrowable == throwable ? throwable : unwrap(undeclaredThrowable);
        }

        if (throwable instanceof InvocationTargetException invocationTargetException) {
            Throwable targetException = invocationTargetException.getTargetException();
            return targetException == throwable ? throwable : unwrap(targetException);
        }

        if (throwable instanceof ExecutionException executionException) {
            Throwable cause = causeOf(executionException);
            return cause == throwable ? throwable : unwrap(cause);
        }

        if (throwable instanceof CompletionException completionException) {
            Throwable cause = causeOf(completionException);
            return cause == throwable ? throwable : unwrap(cause);
        }

        //...

        return throwable;
    }

    @SneakyThrows
    public static Throwable sneakyThrow(Throwable exception) {
        throw exception;
    }
}
