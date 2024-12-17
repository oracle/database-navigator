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
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptExecutionInput;
import com.dbn.execution.script.CmdLineInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Commons.nvl;
import static java.lang.Character.isWhitespace;

public class OracleScriptExecutionInput extends DatabaseScriptExecutionInput {
    private static final String SQLPLUS_CONNECT_PATTERN_TNS= "[USER]@[TNS_PROFILE]";
    private static final String SQLPLUS_CONNECT_PATTERN_SID = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SID=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_SERVICE = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SERVICE_NAME=[DATABASE])))\"";
    private static final String SQLPLUS_CONNECT_PATTERN_BASIC = "[USER]@[HOST]:[PORT]/[DATABASE]";

    public OracleScriptExecutionInput(
            @NotNull CmdLineInterface cmdLineInterface,
            @NotNull String filePath,
            @NotNull String content,
            @Nullable SchemaId schemaId,
            @NotNull DatabaseInfo databaseInfo,
            @NotNull AuthenticationInfo authenticationInfo) {
        super(cmdLineInterface, filePath, adjustContent(content), schemaId, databaseInfo, authenticationInfo);
    }

    private static String adjustContent(String content) {
        content = Strings.trim(content);
        char lastChr = Strings.lastChar(content, chr -> !isWhitespace(chr));
        if (lastChr != ';' && lastChr != '/' && lastChr != ' ') {
            // make sure exit command is not impacted by script errors
            content = content + ";\n";
        }
        return content;
    }

    @Override
    protected void initExecutable(CmdLineInterface cmdLineInterface, DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        String connectionParam = buildConnectionParameter(databaseInfo, authenticationInfo);

        boolean tnsConnection = databaseInfo.getUrlType() == DatabaseUrlType.TNS;
        if (tnsConnection) {
            addEnvironmentVariable("TNS_ADMIN", nvl(databaseInfo.getTnsFolder(), ""));
        }

        String executable = cmdLineInterface.getExecutablePath();
        initCommand(executable);
        addParameter(connectionParam);
    }

    @Override
    protected void initAuthentication(AuthenticationInfo authenticationInfo) {
        AuthenticationType authType = authenticationInfo.getType();
        if (authType == AuthenticationType.USER_PASSWORD) {
            setPassword(authenticationInfo.getPassword());
        }
    }

    @Override
    protected void initConsoleCommands(String filePath, SchemaId schemaId) {
        if (schemaId != null) {
            addStatement("alter session set current_schema = " + schemaId + ";");
        }

        addStatement("set echo on;");
        addStatement("set linesize 32000;");
        addStatement("set pagesize 40000;");
        addStatement("set long 50000;");

        addStatement("@" + filePath);
        addStatement("exit");
    }

    private static String buildConnectionParameter(DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        DatabaseUrlType urlType = databaseInfo.getUrlType();
        String connectPattern =
                urlType == DatabaseUrlType.TNS ? SQLPLUS_CONNECT_PATTERN_TNS :
                urlType == DatabaseUrlType.SID ? SQLPLUS_CONNECT_PATTERN_SID :
                urlType == DatabaseUrlType.SERVICE ? SQLPLUS_CONNECT_PATTERN_SERVICE :
                                    SQLPLUS_CONNECT_PATTERN_BASIC;

        return connectPattern.
                replace("[USER]",        nvl(authenticationInfo.getUser(),     "")).
                replace("[HOST]",        nvl(databaseInfo.getHost(),           "")).
                replace("[PORT]",        nvl(databaseInfo.getPort(),           "")).
                replace("[DATABASE]",    nvl(databaseInfo.getDatabase(),       "")).
                replace("[TNS_PROFILE]", nvl(databaseInfo.getTnsProfile(),     ""));
    }
}
