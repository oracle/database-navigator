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

import com.dbn.common.constant.Constants;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.SchemaId;
import com.dbn.database.CmdLineExecutionInput;
import com.dbn.database.common.DatabaseExecutionInterfaceImpl;
import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.database.postgres.execution.PostgresMethodExecutionProcessor;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.object.DBMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;

public class PostgresExecutionInterface extends DatabaseExecutionInterfaceImpl {
    @Override
    public MethodExecutionProcessor createExecutionProcessor(DBMethod method) {
        return new PostgresMethodExecutionProcessor(method);
    }

    @Override
    public MethodExecutionProcessor createDebugExecutionProcessor(DBMethod method) {
        return null;
    }

    @Override
    public CmdLineExecutionInput createScriptExecutionInput(@NotNull CmdLineInterface cmdLineInterface, @NotNull String filePath, String content, @Nullable SchemaId schemaId, @NotNull DatabaseInfo databaseInfo, @NotNull AuthenticationInfo authenticationInfo) {
        CmdLineExecutionInput input = new CmdLineExecutionInput(content);

        input.initCommand(cmdLineInterface.getExecutablePath());
        input.addCommandArgument("--echo-all");
        input.addCommandArgument("--host=" + databaseInfo.getHost());

        String port = databaseInfo.getPort();
        if (isNotEmpty(port)) {
            input.addCommandArgument("--port=" + port);
        }

        String database = databaseInfo.getDatabase();
        if (isNotEmpty(database)) {
            input.addCommandArgument("--dbname=" + database);
        }

        AuthenticationType authenticationType = authenticationInfo.getType();
        if (Constants.isOneOf(authenticationType, USER, USER_PASSWORD)) {
            input.addCommandArgument("--username=" + authenticationInfo.getUser());
        }


        if (authenticationType != USER_PASSWORD) {
            input.addCommandArgument("--no-password");
        }
        /*else {
            // TODO (verify and cleanup) does no longer seem to be needed since passwords are now sent over STDIN
            input.addEnvironmentVariable("PGPASSWORD", authenticationInfo.getPassword());
        }*/

        if (schemaId != null) {
            input.addStatement("set search_path to " + schemaId + ";");
        }

        input.addStatement("\\i " + filePath);
        input.addStatement("\\q"); // exit

        return input;
    }
}