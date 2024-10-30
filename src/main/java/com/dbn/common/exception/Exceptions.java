package com.dbn.common.exception;

import com.dbn.common.util.Commons;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.dbn.common.util.Strings.cachedLowerCase;

public class Exceptions {
    public static final SQLNonTransientConnectionException DBN_NOT_CONNECTED_EXCEPTION = new SQLNonTransientConnectionException("Not connected to database");

    private Exceptions() {}

    @NotNull
    public static SQLException toSqlException(@NotNull Throwable e) {
        if (e instanceof SQLException) return (SQLException) e;
        return new SQLException(throwableMessage(e), e);
    }

    @NotNull
    public static SQLException toSqlException(@NotNull Throwable e, String s) {
        if (e instanceof SQLException) return (SQLException) e;
        return new SQLException(s + ": [" + e.getClass().getSimpleName() + "] " + e.getMessage(), e);
    }

    @NotNull
    public static SQLTimeoutException toSqlTimeoutException(@NotNull Throwable e, String s) {
        if (e instanceof SQLTimeoutException) return (SQLTimeoutException) e;
        return new SQLTimeoutException(s + ": [" + e.getClass().getSimpleName() + "] " + e.getMessage(), e);
    }

    @NotNull
    public static RuntimeException toRuntimeException(@NotNull Throwable e) {
        if (e instanceof RuntimeException) return (RuntimeException) e;
        return new RuntimeException(throwableMessage(e), e);
    }

    @NotNull
    private static String throwableMessage(@NotNull Throwable e) {
        return Commons.nvl(e.getMessage(), e.getClass().getSimpleName());
    }

    public static <T> T unsupported() {
        throw new UnsupportedOperationException();
    }

    public static TimeoutException timeoutException(long time, TimeUnit timeUnit) {
        return new TimeoutException("Operation timed out after " + time + " " + cachedLowerCase(timeUnit.name()));
    }

    public static Throwable causeOf(Throwable e) {
        return Commons.nvl(e.getCause(), e);
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
}
