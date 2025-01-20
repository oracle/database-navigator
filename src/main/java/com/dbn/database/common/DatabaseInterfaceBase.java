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

import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.statement.CallableStatementOutput;
import com.dbn.database.common.statement.StatementExecutionProcessor;
import com.dbn.database.interfaces.DatabaseInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
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

    @SneakyThrows
    private Element loadDefinition() {
        return XmlContents.fileToElement(getClass(), fileName);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, @NonNls String loaderId, @Nullable Object... arguments) throws SQLException {
        return executeQuery(connection, false, loaderId, arguments);
    }

    protected ResultSet executeQuery(@NotNull DBNConnection connection, boolean forceExecution, @NonNls String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        ResultSet result = executionProcessor.executeQuery(connection, forceExecution, arguments);
        checkDisposed(connection);
        return result;
    }

    protected <T extends CallableStatementOutput> T executeCall(@NotNull DBNConnection connection, @Nullable T outputReader, @NonNls String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        T result = executionProcessor.executeCall(connection, outputReader, arguments);
        checkDisposed(connection);
        return result;
    }

    protected boolean executeStatement(@NotNull DBNConnection connection, @NonNls String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        boolean result = executionProcessor.executeStatement(connection, arguments);
        checkDisposed(connection);
        return result;
    }

    protected void executeUpdate(@NotNull DBNConnection connection, @NonNls String loaderId, @Nullable Object... arguments) throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(loaderId);
        executionProcessor.executeUpdate(connection, arguments);
        checkDisposed(connection);
    }

    @NotNull
    private StatementExecutionProcessor getExecutionProcessor(@NonNls String loaderId) throws SQLException {
        StatementExecutionProcessor executionProcessor = processors.get(loaderId);
        if (executionProcessor == null) {
            DatabaseType databaseType = interfaces.getDatabaseType();
            throw new SQLFeatureNotSupportedException("Feature [" + loaderId + "] not implemented / supported for " + databaseType.getName() + " database type");
        }
        return executionProcessor;
    }

    private void checkDisposed(DBNConnection connection) {
        nd(connection.getProject());
    }
}
