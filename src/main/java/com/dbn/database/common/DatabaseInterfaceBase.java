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

package com.dbn.database.common;

import com.dbn.common.data.Data;
import com.dbn.common.util.Unsafe;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.statement.CallableStatementOutput;
import com.dbn.database.common.statement.StatementExecutionProcessor;
import com.dbn.database.interfaces.DatabaseInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.language.common.QuotePair;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
public abstract class DatabaseInterfaceBase implements DatabaseInterface{
    private final String fileName;
    private final DatabaseInterfaces interfaces;
    protected Map<String, StatementExecutionProcessor> processors = new HashMap<>();

    public DatabaseInterfaceBase(String fileName, DatabaseInterfaces interfaces) {
        this.fileName = fileName;
        this.interfaces = interfaces;
        reset();
    }

    @Override
    public void reset() {
        processors.clear();
        Element root = loadDefinition();
        for (Element child : root.getChildren()) {
            StatementExecutionProcessor executionProcessor = new StatementExecutionProcessor(child, interfaces);
            String id = executionProcessor.getId();
            processors.put(id, executionProcessor);
        }
    }

    protected QuotePair getIdentifierEnquoter(@NotNull DBNConnection connection) {
        QuotePair quotePair = connection.getIdentifierEnquoter();
        if (quotePair != null) return quotePair;

        return interfaces.getCompatibilityInterface().getDefaultIdentifierQuotes();
    }

    @SneakyThrows
    private Element loadDefinition() {
        return XmlContents.fileToElement(getClass(), fileName);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, @NonNls String statementId, @Nullable Object... arguments) throws SQLException {
        return executeQuery(connection, false, statementId, arguments);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, boolean forceExecution, @NonNls String statementId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(statementId);
        ResultSet result = executionProcessor.executeQuery(connection, forceExecution, arguments);
        checkDisposed(connection);
        return result;
    }

    protected <T extends CallableStatementOutput> T executeCall(@NotNull DBNConnection connection, @Nullable T outputReader, @NonNls String statementId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(statementId);
        T result = executionProcessor.executeCall(connection, outputReader, arguments);
        checkDisposed(connection);
        return result;
    }

    @NonNls
    protected int executeStatement(@NotNull DBNConnection connection, @NonNls String statementId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(statementId);
        int updateCount = executionProcessor.executeStatement(connection, arguments);
        checkDisposed(connection);
        return updateCount;
    }

    @NonNls
    protected int executeUpdate(@NotNull DBNConnection connection, @NonNls String statementId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(statementId);
        int updateCount = executionProcessor.executeUpdate(connection, arguments);
        checkDisposed(connection);
        return updateCount;
    }

    @NonNls
    protected void executeSilentUpdate(@NotNull DBNConnection connection, @NonNls String statementId, @Nullable Object... arguments) {
        Unsafe.warned(() -> executeUpdate(connection, statementId, arguments));
    }


    @NotNull
    private StatementExecutionProcessor getExecutionProcessor(@NonNls String statementId) throws SQLException {
        StatementExecutionProcessor executionProcessor = processors.get(statementId);
        if (executionProcessor == null) {
            DatabaseType databaseType = interfaces.getDatabaseType();
            throw new SQLFeatureNotSupportedException("Feature [" + statementId + "] not implemented / supported for " + databaseType.getName() + " database type");
        }
        return executionProcessor;
    }

    private void checkDisposed(DBNConnection connection) {
        nd(connection.getProject());
    }

    protected final boolean getBooleanValue(DBNConnection connection, String statementId, Object... arguments) throws SQLException {
        return Data.asBooleanPrimitive(getSingleValue(connection, statementId, arguments));
    }


    protected final String getSingleValue(DBNConnection connection, String statementId, Object... arguments) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, statementId, arguments);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            Resources.close(resultSet);
        }
        return null;
    }
}
