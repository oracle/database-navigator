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
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.DBContentType;
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class PostgresDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public PostgresDataDefinitionInterface(DatabaseInterfaces provider) {
        super("postgres_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        // TODO SQL-Injection
        return objectTypeId == DatabaseObjectTypeId.VIEW ? "create view " + objectName + " as\n" + code :
                objectTypeId == DatabaseObjectTypeId.FUNCTION ? "create function " + objectName + " as\n" + code :
                        "create or replace\n" + code;
    }



    public String getSessionSqlMode(DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "get-session-sql-mode");
    }

    public void setSessionSqlMode(String sqlMode, DBNConnection connection) throws SQLException {
        if (sqlMode != null) {
            executeQuery(connection, true, "set-session-sql-mode", sqlMode);
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String viewName, String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-view", viewName, code);
    }

    @Override
    public void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-trigger", tableOwner, tableName, triggerName);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            createObject(oldCode, connection);
            throw e;
        }
    }

    @Override
    public void updateObject(String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-object", newCode);
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
    public void createMethod(MethodFactoryInput method, DBNConnection connection) throws SQLException {
        // TODO SQL-Injection
        Project project = method.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption keywordCaseOption = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption objectCaseOption = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dataTypeCaseOption = styleCaseSettings.getDatatypeCaseOption();

        StringBuilder buffer = new StringBuilder();
        String methodType = method.isFunction() ? "function " : "procedure ";
        buffer.append(keywordCaseOption.format(methodType));
        buffer.append(objectCaseOption.format(method.getObjectName()));
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        for (ArgumentFactoryInput argument : method.getArguments()) {
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength,
                    argument.isInput() && argument.isOutput() ? 5 :
                    argument.isInput() ? 2 :
                    argument.isOutput() ? 3 : 0);
        }


        for (ArgumentFactoryInput argument : method.getArguments()) {
            buffer.append("\n    ");
            
            if (!method.isFunction()) {
                String direction =
                        argument.isInput() && argument.isOutput() ? keywordCaseOption.format("inout") :
                                argument.isInput() ? keywordCaseOption.format("in") :
                                        argument.isOutput() ? keywordCaseOption.format("out") : "";
                buffer.append(direction);
                buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            buffer.append(objectCaseOption.format(argument.getObjectName()));
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));

            buffer.append(dataTypeCaseOption.format(argument.getDataType()));
            if (argument != method.getArguments().get(method.getArguments().size() -1)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (method.isFunction()) {
            buffer.append(keywordCaseOption.format("returns "));
            buffer.append(dataTypeCaseOption.format(method.getReturnArgument().getDataType()));
            buffer.append("\n");
        }
        buffer.append(keywordCaseOption.format("begin\n\n"));
        if (method.isFunction()) buffer.append(keywordCaseOption.format("    return null;\n\n"));
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
