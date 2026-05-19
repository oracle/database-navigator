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

import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.DBContentType;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_INPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_OUTPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;
import static com.dbn.object.type.DBObjectType.ARGUMENT;

public class PostgresDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public PostgresDataDefinitionInterface(DatabaseInterfaces provider) {
        super("postgres_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        return objectTypeId == DatabaseObjectTypeId.VIEW ? "create view " + objectName + " as\n" + code :
                objectTypeId == DatabaseObjectTypeId.FUNCTION ? "create function " + objectName + " as\n" + code :
                        "create or replace\n" + code;
    }



    public String getSessionSqlMode(DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "get-session-sql-mode");
    }

    public void setSessionSqlMode(String sqlMode, DBNConnection connection) throws SQLException {
        if (sqlMode != null) {
            executeUpdate(connection, "set-session-sql-mode", sqlMode);
        }
    }

    @Override
    public String extractDDLStatement(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/

    @Override
    public void updateTrigger(String ownerName, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-trigger", ownerName, tableName, triggerName);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            createObject(oldCode, connection);
            throw e;
        }
    }

    @Override
    public void updateObject(String ownerName, String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "update-object", newCode);
    }

    /*********************************************************
     *                     DROP statements                   *
     *********************************************************/
    private void dropTriggerIfExists(String objectName, DBNConnection connection) throws SQLException {

    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(@NotNull DBObjectSpec methodSpec, DBNConnection connection) throws SQLException {
        // TODO SQL-Injection
        Project project = methodSpec.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption keywordCaseOption = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption objectCaseOption = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dataTypeCaseOption = styleCaseSettings.getDatatypeCaseOption();
        boolean function = methodSpec.getObjectType() == DBObjectType.FUNCTION;

        StringBuilder buffer = new StringBuilder();
        String methodType = function ? "function " : "procedure ";
        buffer.append(keywordCaseOption.format(methodType));
        buffer.append(objectCaseOption.format(methodSpec.getObjectName()));
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        DBObjectSpecList<DBObjectSpec> arguments = methodSpec.getChildren(ARGUMENT);
        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength, in && out ? 5 : in ? 2 : out ? 3 : 0);
        }


        for (DBObjectSpec argumentSpec : arguments) {
            boolean in = IS_INPUT.is(argumentSpec);
            boolean out = IS_OUTPUT.is(argumentSpec);

            buffer.append("\n    ");
            if (!function) {
                String direction =
                        in && out ? keywordCaseOption.format("inout") :
                        in ? keywordCaseOption.format("in") :
                        out ? keywordCaseOption.format("out") : "";
                buffer.append(direction);
                buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            buffer.append(objectCaseOption.format(argumentSpec.getObjectName()));
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argumentSpec.getObjectName().length() + 1));

            String dataType = DATA_TYPE.of(argumentSpec);
            buffer.append(dataTypeCaseOption.format(dataType));
            if (argumentSpec != Lists.lastElement(arguments)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (function) {
            DBObjectSpec returnArgument = RETURN_ARGUMENT.of(methodSpec);
            buffer.append(keywordCaseOption.format("returns "));
            buffer.append(dataTypeCaseOption.format(DATA_TYPE.of(returnArgument)));
            buffer.append("\n");
        }
        buffer.append(keywordCaseOption.format("begin\n\n"));
        if (function) {
            buffer.append(keywordCaseOption.format("    return null;\n\n"));
        }
        buffer.append("end");
        
        String sqlMode = getSessionSqlMode(connection);
        try {
            setSessionSqlMode("TRADITIONAL", connection);
            createObject(buffer.toString(), connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }
}
