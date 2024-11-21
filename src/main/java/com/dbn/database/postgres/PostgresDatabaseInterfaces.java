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

package com.dbn.database.postgres;

import com.dbn.connection.DatabaseType;
import com.dbn.database.common.DatabaseEnvironmentInterfaceImpl;
import com.dbn.database.common.DatabaseInterfacesBase;
import com.dbn.database.common.DatabaseNativeDataTypes;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseEnvironmentInterface;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import lombok.Getter;

public class PostgresDatabaseInterfaces extends DatabaseInterfacesBase {
    private final @Getter(lazy = true) DatabaseMessageParserInterface messageParserInterface = new PostgresMessageParserInterface();
    private final @Getter(lazy = true) DatabaseCompatibilityInterface compatibilityInterface = new PostgresCompatibilityInterface();
    private final @Getter(lazy = true) DatabaseEnvironmentInterface environmentInterface = new DatabaseEnvironmentInterfaceImpl();
    private final @Getter(lazy = true) DatabaseMetadataInterface metadataInterface = new PostgresMetadataInterface(this);
    private final @Getter(lazy = true) DatabaseDataDefinitionInterface dataDefinitionInterface = new PostgresDataDefinitionInterface(this);
    private final @Getter(lazy = true) DatabaseExecutionInterface executionInterface = new PostgresExecutionInterface();
    private final @Getter(lazy = true) DatabaseNativeDataTypes nativeDataTypes = new PostgresNativeDataTypes();

    public PostgresDatabaseInterfaces() {
        super(
            SQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.POSTGRES_SQL),
            PSQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.POSTGRES_PSQL));
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRES;
    }
}
