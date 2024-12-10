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

import static com.dbn.common.util.Commons.nvl;

@NonNls
public class OracleExecutionInterface implements DatabaseExecutionInterface {
    private static final String SQLPLUS_CONNECT_PATTERN_TNS= "[USER]@[TNS_PROFILE]";
    private static final String SQLPLUS_CONNECT_PATTERN_SID = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SID=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_SERVICE = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SERVICE_NAME=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_BASIC = "[USER]@[HOST]:[PORT]/[DATABASE]";

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

        CmdLineExecutionInput input = new CmdLineExecutionInput(content);
        DatabaseUrlType urlType = databaseInfo.getUrlType();
        String connectPattern =
                urlType == DatabaseUrlType.TNS ? SQLPLUS_CONNECT_PATTERN_TNS :
                urlType == DatabaseUrlType.SID ? SQLPLUS_CONNECT_PATTERN_SID :
                urlType == DatabaseUrlType.SERVICE ? SQLPLUS_CONNECT_PATTERN_SERVICE :
                SQLPLUS_CONNECT_PATTERN_BASIC;

        String connectArg = connectPattern.
                replace("[USER]",        nvl(authenticationInfo.getUser(),     "")).
                replace("[HOST]",        nvl(databaseInfo.getHost(),           "")).
                replace("[PORT]",        nvl(databaseInfo.getPort(),           "")).
                replace("[DATABASE]",    nvl(databaseInfo.getDatabase(),       "")).
                replace("[TNS_PROFILE]", nvl(databaseInfo.getTnsProfile(),     ""));

        boolean tnsConnection = databaseInfo.getUrlType() == DatabaseUrlType.TNS;
        if (tnsConnection) {
            input.addEnvironmentVariable("TNS_ADMIN", nvl(databaseInfo.getTnsFolder(), ""));
        }

        String executable = cmdLineInterface.getExecutablePath();
        input.initCommand(executable);
        input.addCommandArgument(connectArg);

        if (schemaId != null) {
            input.addStatement("alter session set current_schema = " + schemaId + ";");
        }

        input.addStatement("set echo on;");
        input.addStatement("set linesize 32000;");
        input.addStatement("set pagesize 40000;");
        input.addStatement("set long 50000;");

        input.addStatement("@" + filePath);
        input.addStatement("exit");
        return input;
    }




}
