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

package com.dbn.database;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptCredentialDelivery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.dbn.common.util.FilePermissions.createOwnerOnlyTempFile;

public abstract class DatabaseScriptExecutionInput extends CmdLineExecutionInput{
    private final DatabaseInfo databaseInfo;
    private final File scriptFile;
    private final ScriptCredentialDelivery passwordDeliveryMethod;

    public DatabaseScriptExecutionInput(
            @NotNull ConnectionHandler connection,
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull File scriptFile,
            @NotNull String content,
            @Nullable SchemaId schemaId) {
        super(content);
        this.scriptFile = scriptFile;
        this.passwordDeliveryMethod = ScriptCredentialDelivery.current();

        DatabaseInfo databaseInfo = connection.getDatabaseInfo();
        this.databaseInfo = databaseInfo;
        AuthenticationInfo authenticationInfo = connection.getAuthenticationInfo();
        initExecutable(cmdLineInterface, databaseInfo, authenticationInfo);
        initAuthentication(authenticationInfo);
        initConsoleCommands(scriptFile, schemaId, connection);
    }

    protected abstract void initExecutable(
            CmdLineInterface cmdLineInterface,
            DatabaseInfo databaseInfo,
            AuthenticationInfo authenticationInfo);

    protected abstract void initAuthentication(AuthenticationInfo authenticationInfo);

    protected abstract void initConsoleCommands(File scriptFile, SchemaId schemaId, ConnectionHandler connection);


    protected @NotNull DatabaseInfo getDatabaseInfo() {
        return databaseInfo;
    }

    protected ScriptCredentialDelivery getPasswordDeliveryMethod() {
        return passwordDeliveryMethod;
    }

    protected File createCredentialFile(String prefix, String suffix) throws IOException {
        Path parentDirectory = scriptFile.toPath().getParent();
        if (parentDirectory == null) throw new IOException("Script file has no parent directory: " + scriptFile);

        return createOwnerOnlyTempFile(parentDirectory, prefix, suffix).toFile();
    }

    protected static String getQuotedSchemaId(SchemaId schemaId, ConnectionHandler connection) {
        return connection.getIdentifierCache().getQuotedIdentifier(schemaId.id());
    }
}
