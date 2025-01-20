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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptExecutionInput;
import com.dbn.execution.script.CmdLineInterface;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.connection.AuthenticationType.USER_PASSWORD;

@NonNls
public final class PostgresScriptExecutionInput extends DatabaseScriptExecutionInput {

    public PostgresScriptExecutionInput(
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull String filePath,
            @NotNull String content,
            @Nullable SchemaId schemaId,
            @NotNull DatabaseInfo databaseInfo,
            @NotNull AuthenticationInfo authenticationInfo) {

        super(cmdLineInterface, filePath, content, schemaId, databaseInfo, authenticationInfo);
    }

    @Override
    protected void initExecutable(CmdLineInterface cmdLineInterface, DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        initCommand(cmdLineInterface.getExecutablePath());
        addParameter("--echo-all");
        addKvParameter("--host", databaseInfo.getHost());
        addKvParameter("--port", databaseInfo.getPort());
        addKvParameter("--dbname", databaseInfo.getDatabase());
        addKvParameter("--username", authenticationInfo.getUser());
    }

    @Override
    protected void initAuthentication(AuthenticationInfo authenticationInfo) {
        AuthenticationType authType = authenticationInfo.getType();
        if (authType != USER_PASSWORD) {
            addParameter("--no-password");
        } else {
            addEnvironmentVariable("PGPASSWORD", authenticationInfo.getPassword());
        }
    }

    @Override
    protected void initConsoleCommands(String filePath, SchemaId schemaId) {
        if (schemaId != null) {
            addStatement("set search_path to " + schemaId + ";");
        }

        addStatement("\\i " + filePath);
        addStatement("\\q"); // exit
    }
}
