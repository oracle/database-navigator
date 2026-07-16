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

import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseDriverInterface;
import com.dbn.database.interfaces.DatabaseEnvironmentInterface;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.database.interfaces.DatabaseInterface;
import com.dbn.database.interfaces.DatabaseInterfaceType;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseJavaInterface;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.database.interfaces.DatabaseSchedulerInterface;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.database.interfaces.DatabaseInterfaceType.ASSISTANT;
import static com.dbn.database.interfaces.DatabaseInterfaceType.COMPATIBILITY;
import static com.dbn.database.interfaces.DatabaseInterfaceType.DATA_DEFINITION;
import static com.dbn.database.interfaces.DatabaseInterfaceType.DEBUGGER;
import static com.dbn.database.interfaces.DatabaseInterfaceType.DRIVER;
import static com.dbn.database.interfaces.DatabaseInterfaceType.ENVIRONMENT;
import static com.dbn.database.interfaces.DatabaseInterfaceType.EXECUTION;
import static com.dbn.database.interfaces.DatabaseInterfaceType.JAVA;
import static com.dbn.database.interfaces.DatabaseInterfaceType.MESSAGE_PARSER;
import static com.dbn.database.interfaces.DatabaseInterfaceType.METADATA;
import static com.dbn.database.interfaces.DatabaseInterfaceType.SCHEDULER;
import static com.dbn.database.interfaces.DatabaseInterfaceType.VECTOR;

public abstract class DatabaseInterfacesBase implements DatabaseInterfaces {
    private final SQLLanguageDialect sqlLanguageDialect;
    private final PSQLLanguageDialect psqlLanguageDialect;
    private final Map<DatabaseInterfaceType, DatabaseInterface> interfaces = new ConcurrentHashMap<>();

    protected DatabaseInterfacesBase(SQLLanguageDialect sqlLanguageDialect, @Nullable PSQLLanguageDialect psqlLanguageDialect) {
        this.sqlLanguageDialect = sqlLanguageDialect;
        this.psqlLanguageDialect = psqlLanguageDialect;
    }

    protected abstract DatabaseInterface createInterface(DatabaseInterfaceType interfaceType);

    @Nullable
    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage<?> language) {
        if (language == SQLLanguage.INSTANCE) return sqlLanguageDialect;
        if (language == PSQLLanguage.INSTANCE) return psqlLanguageDialect;
        return null;
    }

    @Override
    public DatabaseDriverInterface getDriverInterface() {
        return getInterface(DRIVER);
    }

    @Override
    public DatabaseMessageParserInterface getMessageParserInterface() {
        return getInterface(MESSAGE_PARSER);
    }

    @Override
    public DatabaseEnvironmentInterface getEnvironmentInterface() {
        return getInterface(ENVIRONMENT);
    }

    @Override
    public DatabaseCompatibilityInterface getCompatibilityInterface() {
        return getInterface(COMPATIBILITY);
    }

    @Override
    public DatabaseMetadataInterface getMetadataInterface() {
        return getInterface(METADATA);
    }

    @Override
    public DatabaseDataDefinitionInterface getDataDefinitionInterface() {
        return getInterface(DATA_DEFINITION);
    }

    @Override
    public DatabaseExecutionInterface getExecutionInterface() {
        return getInterface(EXECUTION);
    }

    @Override
    public DatabaseDebuggerInterface getDebuggerInterface() {
        return getInterface(DEBUGGER);
    }

    @Override
    public DatabaseAssistantInterface getAssistantInterface() {
        return getInterface(ASSISTANT);
    }

    @Override
    public DatabaseVectorInterface getVectorInterface() {
        return getInterface(VECTOR);
    }

    @Override
    public DatabaseSchedulerInterface getSchedulerInterface() {
        return getInterface(SCHEDULER);
    }

    @Override
    public DatabaseJavaInterface getJavaInterface() {
        return getInterface(JAVA);
    }

    @Override
    public void reset() {
        interfaces.values().forEach(DatabaseInterface::reset);
    }


    @SuppressWarnings("unchecked")
    private <T extends DatabaseInterface> T getInterface(DatabaseInterfaceType interfaceType) {
        return (T) interfaces.computeIfAbsent(interfaceType, t -> createRequiredInterface(t));
    }

    private DatabaseInterface createRequiredInterface(DatabaseInterfaceType interfaceType) {
        DatabaseInterface databaseInterface = createInterface(interfaceType);
        if (databaseInterface != null) return databaseInterface;

        throw new UnsupportedOperationException(
                "Database interface " + interfaceType + " is not supported for " + getDatabaseType().getName() + " database type");
    }
}
