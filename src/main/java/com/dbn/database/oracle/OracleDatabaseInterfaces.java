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
import com.dbn.database.interfaces.DatabaseInterface;
import com.dbn.database.interfaces.DatabaseInterfaceType;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import lombok.Getter;

public class OracleDatabaseInterfaces extends DatabaseInterfacesBase {
    private final @Getter(lazy = true) DatabaseNativeDataTypes nativeDataTypes = new OracleNativeDataTypes();


    public OracleDatabaseInterfaces() {
        super(SQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.ORACLE_SQL),
                PSQLLanguage.INSTANCE.getLanguageDialect(DBLanguageDialectIdentifier.ORACLE_PLSQL));
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }

    @Override
    protected DatabaseInterface createInterface(DatabaseInterfaceType interfaceType) {
        return switch (interfaceType) {
            case MESSAGE_PARSER -> new OracleMessageParserInterface();
            case ENVIRONMENT -> new OracleEnvironmentInterface();
            case COMPATIBILITY -> new OracleCompatibilityInterface();
            case METADATA -> new OracleMetadataInterface(this);
            case DATA_DEFINITION -> new OracleDataDefinitionInterface(this);
            case EXECUTION -> new OracleExecutionInterface();
            case DEBUGGER -> new OracleDebuggerInterface(this);
            case ASSISTANT -> new OracleAssistantInterface(this);
            case VECTOR -> new OracleVectorInterface(this);
            case DATA_SOURCE_CONFIG -> new OracleDatasourceConfigInterface(this);
            case SCHEDULER -> new OracleSchedulerInterface(this);
            case MACHINE_LEARNING -> new OracleMachineLearningInterface(this);
            case JAVA -> new OracleJavaInterface(this);
            default -> null;
        };
    }
}
