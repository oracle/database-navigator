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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseScriptClientCommand;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.ScriptExecutionInput;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Files.normalizePath;
import static java.lang.Character.isWhitespace;

@NonNls
public class OracleScriptClientCommand extends DatabaseScriptClientCommand {
    private static final Pattern ORACLE_JDBC_URL_PATTERN = Pattern.compile(
            "^jdbc:oracle:(?:thin|oci):@",
            Pattern.CASE_INSENSITIVE);

    public static final String SQLPLUS_CONNECT_PATTERN_TNS= "[USER]@[TNS_PROFILE]";
    public static final String SQLPLUS_CONNECT_PATTERN_SID = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SID=[DATABASE])))\"";
    public static final String SQLPLUS_CONNECT_PATTERN_SERVICE = "[USER]@\"(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=[HOST])(Port=[PORT]))(CONNECT_DATA=(SERVICE_NAME=[DATABASE])))\"";
    public static final String SQLPLUS_CONNECT_PATTERN_BASIC = "[USER]@[HOST]:[PORT]/[DATABASE]";
    public static final String SQLPLUS_CONNECT_PATTERN_EZCONNECT = "[USER]@[HOST]:[PORT]/[DATABASE]"; // TODO
    public static final String SQLPLUS_CONNECT_PATTERN_CUSTOM = "[USER]@\"[CUSTOM_URL]\"";

    public OracleScriptClientCommand(
            @NotNull ScriptExecutionInput executionInput,
            @NotNull File scriptFile,
            @NotNull String content,
            @Nullable SchemaId schemaId) {
        super(executionInput, scriptFile, adjustContent(content), schemaId);
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
            String tnsAdmin = nvl(databaseInfo.getTnsFolder(), "");
                tnsAdmin = normalizePath(tnsAdmin);
                addEnvironmentVariable("TNS_ADMIN", tnsAdmin);
        }

        String executable = cmdLineInterface.getExecutablePath();
        initCommand(executable);
        addParameter("-L");
        addParameter(connectionParam);
    }

    @Override
    protected void initAuthentication(CmdLineInterface cmdLineInterface, AuthenticationInfo authenticationInfo) {
        AuthenticationType authType = authenticationInfo.getType();
        if (authType == AuthenticationType.USER_PASSWORD) {
            setPassword(authenticationInfo.getPassword());
        }
    }

    @Override
    protected void initConsoleCommands(File scriptFile, SchemaId schemaId, ConnectionHandler connection) {

        if (schemaId != null) {
            addStatement("alter session set current_schema = " + getQuotedSchemaId(schemaId, connection) + ";");
        }

        addStatement("set echo on;");
        addStatement("set linesize 32000;");
        addStatement("set pagesize 40000;");
        addStatement("set long 50000;");

        addStatement("@" + scriptFile.getPath());
        addStatement("exit");
    }

    private static String buildConnectionParameter(DatabaseInfo databaseInfo, AuthenticationInfo authenticationInfo) {
        SqlPlusLogin login = resolveSqlPlusLogin(authenticationInfo);

        DatabaseUrlType urlType = databaseInfo.getUrlType();
        String connectPattern =
                urlType == DatabaseUrlType.TNS ? SQLPLUS_CONNECT_PATTERN_TNS :
                urlType == DatabaseUrlType.SID ? SQLPLUS_CONNECT_PATTERN_SID :
                urlType == DatabaseUrlType.SERVICE ? SQLPLUS_CONNECT_PATTERN_SERVICE :
                urlType == DatabaseUrlType.EZCONNECT ? SQLPLUS_CONNECT_PATTERN_EZCONNECT :
                urlType == DatabaseUrlType.CUSTOM ? SQLPLUS_CONNECT_PATTERN_CUSTOM :
                                    SQLPLUS_CONNECT_PATTERN_BASIC;

        String connectionParameter = connectPattern.
                replace("[USER]",        login.user()).
                replace("[HOST]",        nvl(databaseInfo.getHost(),           "")).
                replace("[PORT]",        nvl(databaseInfo.getPort(),           "")).
                replace("[DATABASE]",    nvl(databaseInfo.getDatabase(),       "")).
                replace("[TNS_PROFILE]", nvl(databaseInfo.getTnsProfile(),     "")).
                replace("[CUSTOM_URL]",  getCustomConnectIdentifier(databaseInfo));

        return connectionParameter + login.adminPrivilegeClause();
    }

    private static String getCustomConnectIdentifier(DatabaseInfo databaseInfo) {
        String url = nvl(databaseInfo.getUrl(), "");
        Matcher matcher = ORACLE_JDBC_URL_PATTERN.matcher(url);
        return matcher.find() ? url.substring(matcher.end()) : url;
    }

    private static SqlPlusLogin resolveSqlPlusLogin(AuthenticationInfo authenticationInfo) {
        String user = nvl(authenticationInfo.getUser(), "");
        String privilege = nvl(authenticationInfo.getAdminPrivilege(), "");
        Matcher matcher = AuthenticationInfo.USER_AS_ADMIN_PRIVILEGE.matcher(user.trim());
        if (matcher.matches()) {
            user = matcher.group(1).trim();
            if (Strings.isEmptyOrSpaces(privilege)) {
                privilege = matcher.group(2);
            }
        }

        String privilegeClause = Strings.isEmptyOrSpaces(privilege) ? "" :
                " AS " + privilege.trim().toUpperCase(Locale.ENGLISH);
        return new SqlPlusLogin(user, privilegeClause);
    }

    private record SqlPlusLogin(String user, String adminPrivilegeClause) {}
}
