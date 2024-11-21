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

package com.dbn.database.oracle;

import com.dbn.connection.DatabaseType;
import com.dbn.database.common.DatabaseInterfacesBase;
import com.dbn.database.common.DatabaseNativeDataTypes;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseEnvironmentInterface;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import lombok.Getter;

public class OracleDatabaseInterfaces extends DatabaseInterfacesBase {
    private final @Getter(lazy = true) DatabaseMessageParserInterface messageParserInterface = new OracleMessageParserInterface();
    private final @Getter(lazy = true) DatabaseCompatibilityInterface compatibilityInterface = new OracleCompatibilityInterface();
    private final @Getter(lazy = true) DatabaseEnvironmentInterface environmentInterface = new OracleEnvironmentInterface();
    private final @Getter(lazy = true) DatabaseMetadataInterface metadataInterface = new OracleMetadataInterface(this);
    private final @Getter(lazy = true) DatabaseDebuggerInterface debuggerInterface = new OracleDebuggerInterface(this);
    private final @Getter(lazy = true) DatabaseAssistantInterface assistantInterface = new OracleAssistantInterface(this);
    private final @Getter(lazy = true) DatabaseDataDefinitionInterface dataDefinitionInterface = new OracleDataDefinitionInterface(this);
    private final @Getter(lazy = true) DatabaseExecutionInterface executionInterface = new OracleExecutionInterface();
    private final @Getter(lazy = true) DatabaseNativeDataTypes nativeDataTypes = new OracleNativeDataTypes();


    public OracleDatabaseInterfaces() {
        super(SQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.ORACLE_SQL),
                PSQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.ORACLE_PLSQL));
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }
}