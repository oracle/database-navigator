package com.dbn.liquibase.execution;

import com.dbn.common.routine.ThrowableFunction;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.jdbc.DBNConnection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.exception.Exceptions.unwrap;

/** Coordinates execution of a Liquibase input and publishes its execution result. */
@Getter
public class LiquibaseExecutionProcessor {
    private final LiquibaseExecutionInput input;
    private LiquibaseExecutionResult result;

    public LiquibaseExecutionProcessor(@NotNull LiquibaseExecutionInput input) {
        this.input = input;
    }

    @NotNull
    public LiquibaseExecutionResult prepareExecutionResult() {
        if (result == null) {
            result = new LiquibaseExecutionResult(
                    input.getConnection(),
                    input.getOperation());
        }
        return result;
    }

    @NotNull
    public LiquibaseExecutionResult execute() {
        LiquibaseExecutionResult result = prepareExecutionResult();
        result.start();
        return result;
    }

    protected <T> T withPoolConnection(boolean readonly, @NotNull ThrowableFunction<DBNConnection, T, Exception> operation) throws SQLException {
        ConnectionHandler connection = input.getConnection();
        ConnectionContext context = new ConnectionContext(
                connection.getProject(),
                connection.getConnectionId(),
                null);
        return PooledConnection.call(context, readonly, c -> {
            try {
                return operation.apply(c);
            } catch (Throwable e) {
                throw toSqlException(unwrap(e));
            }
        });
    }
}
