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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.SchemaId;
import com.dbn.database.CmdLineExecutionInput;
import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.database.oracle.execution.OracleMethodDebugExecutionProcessor;
import com.dbn.database.oracle.execution.OracleMethodExecutionProcessor;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.object.DBMethod;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.util.Commons.nvl;
import static java.lang.Character.isWhitespace;

@NonNls
public class OracleExecutionInterface implements DatabaseExecutionInterface {
    private static final String SQLPLUS_CONNECT_PATTERN_TNS= "[USER]/[PASSWORD]@[TNS_PROFILE]";
    private static final String SQLPLUS_CONNECT_PATTERN_SID = "[USER]/[PASSWORD]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SID=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_SERVICE = "[USER]/[PASSWORD]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SERVICE_NAME=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_BASIC = "[USER]/[PASSWORD]@[HOST]:[PORT]/[DATABASE]";

    @Override
    public MethodExecutionProcessor createExecutionProcessor(DBMethod method) {
        return new OracleMethodExecutionProcessor(method);
    }

    @Override
    public MethodExecutionProcessor createDebugExecutionProcessor(DBMethod method) {
        return new OracleMethodDebugExecutionProcessor(method);
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
        DatabaseUrlType urlType = databaseInfo.getUrlType();
        String connectPattern =
                urlType == DatabaseUrlType.TNS ? SQLPLUS_CONNECT_PATTERN_TNS :
                urlType == DatabaseUrlType.SID ? SQLPLUS_CONNECT_PATTERN_SID :
                urlType == DatabaseUrlType.SERVICE ? SQLPLUS_CONNECT_PATTERN_SERVICE :
                SQLPLUS_CONNECT_PATTERN_BASIC;

        String connectArg = connectPattern.
                replace("[USER]",        nvl(authenticationInfo.getUser(),     "")).
                replace("[PASSWORD]",    nvl(authenticationInfo.getPassword(), "")).
                replace("[HOST]",        nvl(databaseInfo.getHost(),           "")).
                replace("[PORT]",        nvl(databaseInfo.getPort(),           "")).
                replace("[DATABASE]",    nvl(databaseInfo.getDatabase(),       "")).
                replace("[TNS_PROFILE]", nvl(databaseInfo.getTnsProfile(),     ""));

        executionInput.addEnvironmentVariable("TNS_ADMIN", nvl(databaseInfo.getTnsFolder(),      ""));

        String fileArg = "\"@" + filePath + "\"";



        @NonNls List<String> command = executionInput.getCommand();
        command.add(cmdLineInterface.getExecutablePath());
        command.add(connectArg);
        command.add(fileArg);

        @NonNls
        StringBuilder builder = executionInput.getContent();
        if (schemaId != null) builder.insert(0, "alter session set current_schema = " + schemaId + ";\n");

        builder.insert(0, "set echo on;\n");
        builder.insert(0, "set linesize 32000;\n");
        builder.insert(0, "set pagesize 40000;\n");
        builder.insert(0, "set long 50000;\n");

        Strings.trim(builder);
        char lastChr = Strings.lastChar(builder, chr -> !isWhitespace(chr));
        if (lastChr != ';' && lastChr != '/' && lastChr != ' ') {
            // make sure exit is not impacted by script errors
            builder.append(";\n");
        }
        builder.append("\nexit;\n");
        return executionInput;
    }




}
