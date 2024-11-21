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
import com.dbn.common.util.Strings;
import com.dbn.connection.SchemaId;
import com.dbn.database.CmdLineExecutionInput;
import com.dbn.database.common.DatabaseExecutionInterfaceImpl;
import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.object.DBMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MySqlExecutionInterface extends DatabaseExecutionInterfaceImpl {

    @Override
    public MethodExecutionProcessor createExecutionProcessor(DBMethod method) {
        return createSimpleMethodExecutionProcessor(method);
    }

    @Override
    public MethodExecutionProcessor createDebugExecutionProcessor(DBMethod method) {
        return null;
    }

    @Override
    public CmdLineExecutionInput createScriptExecutionInput(
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull String filePath,
            String content,
            @Nullable SchemaId schemaId,
            @NotNull DatabaseInfo databaseInfo,
            @NotNull AuthenticationInfo authenticationInfo) {

        CmdLineExecutionInput executionInput = new CmdLineExecutionInput(content);

        List<String> command = executionInput.getCommand();
        command.add(cmdLineInterface.getExecutablePath());
        command.add("--force");
        command.add("--verbose");
        command.add("--host=" + databaseInfo.getHost());

        String port = databaseInfo.getPort();
        if (Strings.isNotEmpty(port)) {
            command.add("--port=" + port);
        }

        String database = databaseInfo.getDatabase();
        if (Strings.isNotEmpty(database)) {
            command.add("--database=" + database);
        }


        command.add("--user=" + authenticationInfo.getUser());
        command.add("--password=" + authenticationInfo.getPassword());

        command.add("-e");
        command.add("\"source " + filePath + "\"");

        //command.add("< " + filePath);

        StringBuilder contentBuilder = executionInput.getContent();
        if (schemaId != null) {
            contentBuilder.insert(0, "use " + schemaId + ";\n");
        }
        //contentBuilder.append("\nexit;\n");
        return executionInput;
    }
}