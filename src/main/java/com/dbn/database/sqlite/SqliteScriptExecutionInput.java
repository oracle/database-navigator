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

package com.dbn.database.sqlite;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptExecutionInput;
import com.dbn.execution.script.CmdLineInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Naming.doubleQuoted;

public final class SqliteScriptExecutionInput extends DatabaseScriptExecutionInput {
    public SqliteScriptExecutionInput(
            @NotNull ConnectionHandler connection,
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull String filePath,
            @NotNull String content,
            @Nullable SchemaId schemaId) {
        super(connection, cmdLineInterface, filePath, content, schemaId);
    }

    @Override
    protected void initExecutable(CmdLineInterface cmdLineInterface, DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        String executable = cmdLineInterface.getExecutablePath();
        initCommand(executable);

        String databaseFilePath = databaseInfo.getMainFilePath();
        addParameter(databaseFilePath);
    }

    @Override
    protected void initAuthentication(AuthenticationInfo authenticationInfo) {
    }

    @Override
    protected void initConsoleCommands(String filePath, SchemaId schemaId, ConnectionHandler connection) {
        addStatement(".read " + doubleQuoted(filePath));
        addStatement(".exit");
    }
}
