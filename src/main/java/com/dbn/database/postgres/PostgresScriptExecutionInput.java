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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptExecutionInput;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptCredentialDelivery;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static java.nio.charset.StandardCharsets.UTF_8;

@NonNls
public final class PostgresScriptExecutionInput extends DatabaseScriptExecutionInput {
    public PostgresScriptExecutionInput(
            @NotNull ConnectionHandler connection,
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull File scriptFile,
            @NotNull String content,
            @Nullable SchemaId schemaId) {
        super(connection, cmdLineInterface, scriptFile, content, schemaId);
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
        } else if (getPasswordDeliveryMethod() == ScriptCredentialDelivery.TEMP_FILE) {
            addEnvironmentVariable("PGPASSFILE", createPasswordFile(authenticationInfo).getPath());
        } else {
            initLegacyEnvironmentAuthentication(authenticationInfo);
        }
    }

    // Legacy support path enabled only with -Ddbn.script.credentials.delivery=environment.
    private void initLegacyEnvironmentAuthentication(AuthenticationInfo authenticationInfo) {
        addEnvironmentVariable("PGPASSWORD", authenticationInfo.getPassword());
    }

    private File createPasswordFile(AuthenticationInfo authenticationInfo) {
        try {
            DatabaseInfo databaseInfo = getDatabaseInfo();
            File file = createCredentialFile("DBN-postgres-", ".pgpass");
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), UTF_8)) {
                writeEscapedField(writer, getConnectionField(databaseInfo.getHost()));
                writer.write(':');
                writeEscapedField(writer, getConnectionField(databaseInfo.getPort()));
                writer.write(':');
                writeEscapedField(writer, getConnectionField(databaseInfo.getDatabase()));
                writer.write(':');
                writeEscapedField(writer, getConnectionField(authenticationInfo.getUser()));
                writer.write(':');
                writeEscapedField(writer, authenticationInfo.getPassword());
                writer.newLine();
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create PostgreSQL script credential file", e);
        }
    }

    private static char[] getConnectionField(String value) {
        return value == null || value.isBlank() ? new char[]{'*'} : value.toCharArray();
    }

    static void writeEscapedField(BufferedWriter writer, char[] value) throws IOException {
        if (value == null) return;

        for (char c : value) {
            if (c == '\\' || c == ':') {
                writer.write('\\');
            }
            writer.write(c);
        }
    }

    @Override
    protected void initConsoleCommands(File scriptFile, SchemaId schemaId, ConnectionHandler connection) {
        if (schemaId != null) {
            addStatement("set search_path to " + getQuotedSchemaId(schemaId, connection) + ";");
        }

        addStatement("\\i " + scriptFile.getPath());
        addStatement("\\q"); // exit
    }
}
