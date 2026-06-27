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

package com.dbn.database.mysql;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptClientCommand;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptExecutionInput;
import com.dbn.execution.script.ScriptPasswordDelivery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import static com.dbn.execution.script.ScriptPasswordDelivery.CREDENTIAL_FILE;
import static com.dbn.execution.script.ScriptPasswordDelivery.ENVIRONMENT_VARIABLE;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class MySqlScriptClientCommand extends DatabaseScriptClientCommand {
    public MySqlScriptClientCommand(
            @NotNull ScriptExecutionInput executionInput,
            @NotNull File scriptFile,
            @NotNull String content,
            @Nullable SchemaId schemaId) {
        super(executionInput, scriptFile, content, schemaId);
    }

    @Override
    protected void initExecutable(CmdLineInterface cmdLineInterface, DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        String executable = cmdLineInterface.getExecutablePath();
        initCommand(executable);

        addKvParameter("--user", authenticationInfo.getUser());
        addKvParameter("--host", databaseInfo.getHost());

        addKvParameter("--port", databaseInfo.getPort());
        addKvParameter("--database", databaseInfo.getDatabase());
        addParameter("--verbose");
    }

    @Override
    protected void initAuthentication(CmdLineInterface cmdLineInterface, AuthenticationInfo authenticationInfo) {
        AuthenticationType authType = authenticationInfo.getType();
        if (authType == AuthenticationType.USER_PASSWORD) {
            ScriptPasswordDelivery passwordDelivery = getExecutionInput().getPasswordDelivery();
            if (passwordDelivery == CREDENTIAL_FILE) {
                File passwordFile = createPasswordFile(authenticationInfo);
                insertKvParameter("--defaults-extra-file", passwordFile.getPath());
            } else if (passwordDelivery ==  ENVIRONMENT_VARIABLE) {
                // Legacy support path enabled only with -Ddbn.script.credentials.delivery=ENVIRONMENT_VARIABLE.
                addEnvironmentVariable("MYSQL_PWD", authenticationInfo.getPassword());
            }
        }
    }

    private File createPasswordFile(AuthenticationInfo authenticationInfo) {
        try {
            File file = createCredentialFile("DBN-mysql-", ".cnf");
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), UTF_8)) {
                writer.write("[client]");
                writer.newLine();
                writer.write("password=\"");
                writeEscapedOptionValue(writer, authenticationInfo.getPassword());
                writer.write('"');
                writer.newLine();
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create MySQL script credential file", e);
        }
    }

    static void writeEscapedOptionValue(BufferedWriter writer, char[] value) throws IOException {
        if (value == null) return;

        for (char c : value) {
            switch (c) {
                case '\\' -> writer.write("\\\\");
                case '"' -> writer.write("\\\"");
                case '\b' -> writer.write("\\b");
                case '\t' -> writer.write("\\t");
                case '\n' -> writer.write("\\n");
                case '\r' -> writer.write("\\r");
                default -> writer.write(c);
            }
        }
    }

    @Override
    protected void initConsoleCommands(File scriptFile, SchemaId schemaId, ConnectionHandler connection) {
        if (schemaId != null) {
            addStatement("use " + getQuotedSchemaId(schemaId, connection) + ";");
        }
        String filePath = scriptFile.getPath().replace("\\", "/"); // mysql does not seem to understand backslash path even on windows ()
        addStatement("source " + filePath + ";");
        addStatement("exit");
    }
}
