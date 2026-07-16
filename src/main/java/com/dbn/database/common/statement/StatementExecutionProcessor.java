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

import com.dbn.common.compatibility.Exploitable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Compactables;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.database.DatabaseActivityTrace;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.dbn.common.options.setting.Settings.doubleAttribute;
import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.diagnostics.Diagnostics.isDatabaseAccessDebug;

@Slf4j
@Getter
public class StatementExecutionProcessor {
    public static final SQLFeatureNotSupportedException NO_STATEMENT_DEFINITION_EXCEPTION = new SQLFeatureNotSupportedException("No statement definition found");

    private final DatabaseInterfaces interfaces;
    private final String id;
    private final int timeout;
    private List<StatementDefinition> statementDefinitions = new ArrayList<>();


    public StatementExecutionProcessor(Element element, DatabaseInterfaces interfaces) {
        this.interfaces = interfaces;
        this.id = stringAttribute(element, "id");
        this.timeout =  integerAttribute(element, "timeout", 30);

        List<Element> children = element.getChildren();
        if (children.isEmpty()) {
            String statementText = element.getTextTrim();
            readStatements(statementText, null, null, 0.0);
        } else {
            for (Element child : children) {
                String statementText = child.getTextTrim();
                double sinceVersion = doubleAttribute(child, "since-version", 0.0);
                String prefixes = stringAttribute(child, "prefixes");
                String silentErrorCodes = stringAttribute(child, "silent-error-codes");
                readStatements(statementText, prefixes, silentErrorCodes, sinceVersion);
            }
        }
        statementDefinitions = Compactables.compact(statementDefinitions);
    }

    private void readStatements(String statementText, String prefixes, String silentErrorCodes, double sinceVersion) {
        if (prefixes == null) {
            StatementDefinition statementDefinition = new StatementDefinition(statementText, null, silentErrorCodes, sinceVersion);
            statementDefinitions.add(statementDefinition);
        } else {
            StringTokenizer tokenizer = new StringTokenizer(prefixes, ",");
            while (tokenizer.hasMoreTokens()) {
                String prefix = tokenizer.nextToken().trim();
                StatementDefinition statementDefinition = new StatementDefinition(statementText, prefix, silentErrorCodes, sinceVersion);
                statementDefinitions.add(statementDefinition);
            }
        }
    }

    private List<StatementDefinition> getStatementDefinitions(DBNConnection connection) {
        ConnectionHandler database = connection.getConnectionHandler();
        if (database == null) return statementDefinitions;

        double databaseVersion = database.getDatabaseVersion();
        return Lists.filter(statementDefinitions, d -> d.supports(databaseVersion));
    }

    public ResultSet executeQuery(DBNConnection connection, boolean forceExecution, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
        for (StatementDefinition statementDefinition : getStatementDefinitions(connection)) {
            try {
                return executeQuery(statementDefinition, context, forceExecution, arguments);
            } catch (SQLRecoverableException e){
                conditionallyLog(e);
                exception = e;
                break;
            } catch (SQLException e){
                conditionallyLog(e);
                exception = e;
            }
        }
        throw exception;
    }

    private ResultSet executeQuery(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            boolean force,
            Object... arguments) throws SQLException {

        DatabaseCompatibility compatibility = ConnectionHandler.local().getCompatibility();
        DatabaseActivityTrace activityTrace = compatibility.getActivityTrace(definition.getId());

        boolean canExecute = force || activityTrace.canExecute();
        if (!canExecute) {
            throw Commons.nvl(
                    activityTrace.getException(),
                    () -> new SQLException("Too many failed attempts of executing query '" + id + "'."));
        }

        return StatementExecutor.execute(context,
                () -> {
                    DBNConnection connection = context.getConnection();
                    String statementLogText = definition.prepareStatementLogText();
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: {}", statementLogText);

                    DBNPreparedStatement statement = null;
                    ResultSet resultSet = null;
                    try {
                        activityTrace.init();
                        statement = definition.prepareStatement(connection, arguments);
                        context.setStatement(statement);

                        statement.setQueryTimeout(timeout);
                        resultSet = statement.executeQuery();

                        context.log("FETCH_BLOCK", false, false, resultSet.getFetchSize());
                        DBNResultSet.setIdentifier(resultSet, context.getIdentifier());

                        activityTrace.reset();
                        return resultSet;
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        Resources.close(statement);
                        String message = e.getMessage();
                        if (isDatabaseAccessDebug())
                            log.warn("[DBN] Error executing statement: {}\nCause: {}", statementLogText, message);

                        boolean unsupported = interfaces.getMessageParserInterface().isModelException(e);
                        String traceMessage = unsupported ?
                                "Model exception received while executing query '" + id +"'. " + message :
                                "Too many failed attempts of executing query '" + id +"'. " + message;

                        SQLException traceException = new SQLException(traceMessage, e.getSQLState(), e.getErrorCode(), e);

                        activityTrace.fail(traceException, unsupported);
                        throw e;
                    } finally {
                        if (resultSet == null && statement != null) {
                            if (statement.isCached()) {
                                statement.park();
                            } else {
                                Resources.close(statement);
                            }

                        }
                    }
                });
    }

    public <T extends CallableStatementOutput> T executeCall(
            @NotNull DBNConnection connection,
            @Nullable T outputReader,
            Object... arguments) throws SQLException {

        StatementExecutorContext context = createContext(connection);
        SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
        for (StatementDefinition definition : getStatementDefinitions(connection)) {
            try {
                return executeCall(definition, context, outputReader, arguments);
            } catch (SQLException e){
                conditionallyLog(e);
                exception = e;
            }
        }
        throw exception;
    }

    @Exploitable
    private <T extends CallableStatementOutput> T executeCall(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            @Nullable T outputReader,
            Object... arguments) throws SQLException {

        return StatementExecutor.execute(context,
                () -> {
                    DBNConnection connection = context.getConnection();
                    String statementLogText = definition.prepareStatementLogText();
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: {}", statementLogText);

                    DBNCallableStatement statement = null;
                    try {
                        statement = definition.prepareCall(connection, arguments);
                        initOutputReader(outputReader, statement, definition.getParameterCount());

                        context.setStatement(statement);
                        statement.setQueryTimeout(timeout);
                        statement.execute();

                        invokeOutputReader(outputReader, statement);
                        return outputReader;
                    } catch (SQLException e) {
                        handleException(e, statementLogText);
                        return outputReader;
                    } finally {
                        Resources.close(statement);
                    }
                });
    }

    private static <T extends CallableStatementOutput> void initOutputReader(@Nullable T outputReader, DBNCallableStatement statement, int parameterShift) throws SQLException {
        if (outputReader == null) return;

        outputReader.shiftParameterIndex(parameterShift);
        outputReader.registerParameters(statement);
    }

    private static <T extends CallableStatementOutput> void invokeOutputReader(@Nullable T outputReader, DBNCallableStatement statement) throws SQLException {
        if (outputReader == null) return;
        outputReader.read(statement);
    }

    /**
     * Renders the statement text with identifiers and value literals inlined, without executing it.
     * Intended for deferred execution contexts (e.g. a DBMS_SCHEDULER job action) where the finished
     * statement text must be handed to the database instead of run as a JDBC prepared statement.
     * <p>
     * Safe dynamic markers for rendered templates:
     * <ul>
     * <li>{@code {@N}} - identifiers (table/column/object names), safely quoted</li>
     * <li>{@code {$N}} - typed value literals (strings escaped, numbers validated, dates as ANSI
     *     literals), rendered by {@link SqlLiterals} with a strict fail-closed type whitelist</li>
     * </ul>
     * A template carrying {@code {#N}} JDBC bind parameters is rejected, because those cannot be bound
     * in a deferred session. {@code {N}} placeholders are inlined verbatim (no escaping), so they must
     * only ever carry trusted tokens - never untrusted values.
     */
    public String prepareStatementText(@NotNull DBNConnection connection, Object... arguments) throws SQLException {
        for (StatementDefinition definition : getStatementDefinitions(connection)) {
            if (definition.getParameterCount() > 0)
                throw new SQLException("Statement '" + id + "' cannot be rendered for deferred execution: it declares {#N} bind parameters. Use {@N} identifiers or literal text instead.");
            return definition.prepareStatementText(connection, arguments);
        }
        throw NO_STATEMENT_DEFINITION_EXCEPTION;
    }

    public int executeUpdate(DBNConnection connection, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
        for (StatementDefinition statementDefinition : statementDefinitions) {
            try {
                return executeUpdate(statementDefinition, context, arguments);
            } catch (SQLException e){
                conditionallyLog(e);
                exception = e;

                if (statementDefinition.isSilentError(e)) return 0;
            }
        }
        throw exception;
    }

    private int executeUpdate(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            Object... arguments) throws SQLException {
        return StatementExecutor.execute(context,
                () -> {
                    DBNConnection connection = context.getConnection();
                    String statementLogText = definition.prepareStatementLogText();
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: {}", statementLogText);

                    DBNPreparedStatement statement = null;
                    try {
                        statement = definition.prepareStatement(connection, arguments);
                        context.setStatement(statement);

                        statement.setQueryTimeout(timeout);
                        statement.executeUpdate();
                        return statement.getUpdateCount();
                    } catch (SQLException e) {
                        handleException(e, statementLogText);
                    } finally {
                        Resources.close(statement);
                    }
                    return 0;
                });
    }

    public int executeStatement(@NotNull DBNConnection connection, Object... arguments) throws SQLException {
        StatementExecutorContext context = createContext(connection);
        SQLException exception = NO_STATEMENT_DEFINITION_EXCEPTION;
        for (StatementDefinition statementDefinition : getStatementDefinitions(connection)) {
            try {
                return executeStatement(statementDefinition, context, arguments);
            } catch (SQLException e){
                conditionallyLog(e);
                exception = e;

                if (statementDefinition.isSilentError(e)) return 0;
            }
        }
        throw exception;
    }

    private int executeStatement(
            @NotNull StatementDefinition definition,
            @NotNull StatementExecutorContext context,
            Object... arguments) throws SQLException {
        return StatementExecutor.execute(context,
                () -> {
                    DBNConnection connection = context.getConnection();
                    String statementText = definition.prepareStatementText(connection, arguments);
                    String statementLogText = definition.prepareStatementLogText();
                    if (isDatabaseAccessDebug()) log.info("[DBN] Executing statement: {}", statementLogText);

                    DBNStatement statement = connection.createStatement();
                    context.setStatement(statement);
                    try {
                        statement.setQueryTimeout(timeout);
                        statement.execute(statementText);
                        return statement.getUpdateCount();
                    } catch (SQLException e) {
                        handleException(e, statementLogText);
                        return 0;
                    } finally {
                        Resources.close(statement);
                    }
                });
    }

    private void handleException(SQLException e, String statementLogText) throws SQLException {
        conditionallyLog(e);
        if (isSuccessException(e)) {
            log.warn("[DBN] Success exception received while executing statement \"{}\"\nDetails: {}", statementLogText, e.getMessage());
            return;
        }

        if (isDatabaseAccessDebug()) {
            log.warn("[DBN] Error executing statement: {}\nDetails: {}", statementLogText, e.getMessage());
        }
        throw e;
    }

    private boolean isSuccessException(SQLException e) {
        DatabaseMessageParserInterface parserInterface = interfaces.getMessageParserInterface();
        return parserInterface.isSuccessException(e);
    }

    @NotNull
    public StatementExecutorContext createContext(@NotNull DBNConnection connection) {
        return new StatementExecutorContext(connection, id, timeout);
    }
}
