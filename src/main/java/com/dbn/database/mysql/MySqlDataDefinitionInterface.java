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

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.common.util.Strings;
import com.dbn.connection.Resources;
import com.dbn.connection.ResultSets;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.common.quotes.QuotePair;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.type.DBConstraintType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.util.Lists.lastElement;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.language.common.quotes.QuoteEscaping.DATABASE;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_INPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_OUTPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;
import static com.dbn.object.type.DBObjectType.ARGUMENT;

public class MySqlDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public MySqlDataDefinitionInterface(DatabaseInterfaces provider) {
        super("mysql_ddl_interface.xml", provider);
    }


    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        schemaName = quoted(schemaName);
        objectName = quoted(objectName);

        if (isEmpty(alternativeDelimiter)) {
            alternativeDelimiter = getInterfaces().getCompatibilityInterface().getDefaultAlternativeStatementDelimiter();
        }

        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings caseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();


        if (objectTypeId == DatabaseObjectTypeId.VIEW) {
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " view ") +
                    (useQualified ? schemaName + "." : "") + objectName +
                    kco.format(" as\n") +
                    code;
        }

        if (objectTypeId.isOneOf(DatabaseObjectTypeId.PROCEDURE, DatabaseObjectTypeId.FUNCTION, DatabaseObjectTypeId.DATASET_TRIGGER)) {
            if (objectTypeId == DatabaseObjectTypeId.DATASET_TRIGGER) {
                objectTypeId = DatabaseObjectTypeId.TRIGGER;
            }
            String objectType = cachedLowerCase(objectTypeId.toString());
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, caseSettings);
            String delimiterChange = kco.format("delimiter ") + alternativeDelimiter + "\n";
            String dropStatement =
                    kco.format("drop " + objectType + " if exists ") +
                    (useQualified ? schemaName + "." : "") + objectName + alternativeDelimiter + "\n";
            String createStatement = kco.format("create definer=current_user\n") + code + alternativeDelimiter + "\n";
            String delimiterReset = kco.format("delimiter ;");
            return delimiterChange + (makeRerunnable ? dropStatement : "") + createStatement + delimiterReset;
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        super.computeSourceCodeOffsets(content, objectTypeId, objectName);
    }

    public String getSessionSqlMode(DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "get-session-sql-mode");
    }

    public void setSessionSqlMode(String sqlMode, DBNConnection connection) throws SQLException {
        if (sqlMode != null) {
            executeCall(connection, null, "set-session-sql-mode", sqlMode);
        }
    }

    @Override
    public String extractDDLStatement(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, "extract-ddl-statement", objectType, ownerName, objectName);
            resultSet.next();
            List<String> columnNames = ResultSets.getColumnNames(resultSet);
            for (String columnName : columnNames) {
                if (columnName.equalsIgnoreCase("create " + objectType)) {
                    return resultSet.getString(columnName);
                }

            }
            throw new SQLException("Cannot extract DDL statement");
        } finally {
            Resources.close(resultSet);
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        try {
            // try instructions
            String tempViewName = getTempObjectName("VIEW");
            dropObjectIfExists("VIEW", ownerName, tempViewName, connection);
            createView(tempViewName, code, connection);
            dropObjectIfExists("VIEW", ownerName, tempViewName, connection);

            // instructions
            dropObjectIfExists("VIEW", ownerName, viewName, connection);
            createView(viewName, code, connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    @Override
    public void updateTrigger(String ownerName, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        // triggers do not support multiple triggers with same event (i.e can not use "try temp" approach)
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        dropObjectIfExists("trigger", ownerName, triggerName, connection);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            createObject(oldCode, connection);
            throw e;
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    @Override
    public void updateObject(String ownerName, String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        try {
            String tempObjectName = getTempObjectName(objectType);
            dropObjectIfExists(objectType, ownerName, tempObjectName, connection);

            QuotePair quotePair = getIdentifierEnquoter(connection);
            String rawObjectName = quotePair.unquote(objectName, DATABASE);

            createObject(newCode.replaceFirst("(?i)" + rawObjectName, tempObjectName), connection);
            dropObjectIfExists(objectType, ownerName, tempObjectName, connection);

            dropObjectIfExists(objectType, ownerName, objectName, connection);
            createObject(newCode, connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    /*********************************************************
     *                     DROP statements                   *
     *********************************************************/

    @Override
    public void dropConstraint(String ownerName, String tableName, String constraintName, DBConstraintType constraintType, DBNConnection connection) throws SQLException {
        switch (constraintType) {
            case PRIMARY_KEY: executeUpdate(connection, "drop-primary-key-constraint", ownerName, tableName); break;
            case FOREIGN_KEY: executeUpdate(connection, "drop-foreign-key-constraint", ownerName, tableName, constraintName); break;
            case UNIQUE_KEY: executeUpdate(connection, "drop-index-constraint", ownerName, tableName, constraintName); break;
            case CHECK: executeUpdate(connection, "drop-check-constraint", ownerName, tableName, constraintName); break;
        }
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(@NotNull DBObjectSpec methodSpec, DBNConnection connection) throws SQLException {
        Project project = methodSpec.getSchema().getProject();
        CodeStyleCaseSettings caseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption dco = caseSettings.getDatatypeCaseOption();
        boolean function = methodSpec.getObjectType() == DBObjectType.FUNCTION;

        StringBuilder buffer = new StringBuilder();
        String methodType = function ? "function " : "procedure ";
        buffer.append(kco.format(methodType));
        buffer.append(methodSpec.getAdjustedObjectName());
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        DBObjectSpecList<DBObjectSpec> arguments = methodSpec.getChildren(ARGUMENT);
        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);

            String argumentName = argument.getAdjustedObjectName();
            maxArgNameLength = Math.max(maxArgNameLength, argumentName.length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength, in && out ? 5 : in ? 2 : out ? 3 : 0);
        }

        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);

            buffer.append("\n    ");
            
            if (!function) {
                String direction =
                        in && out ? kco.format("inout") :
                        in ? kco.format("in") :
                        out ? kco.format("out") : "";
                buffer.append(direction);
                buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            String argumentName = argument.getAdjustedObjectName();
            buffer.append(argumentName);
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argumentName.length() + 1));

            buffer.append(dco.format(DATA_TYPE.of(argument)));
            if (argument != lastElement(arguments)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (function) {
            DBObjectSpec returnArgument = RETURN_ARGUMENT.of(methodSpec);

            buffer.append(kco.format("returns "));
            buffer.append(dco.format(DATA_TYPE.of(returnArgument)));
            buffer.append("\n");
        }
        buffer.append(kco.format("begin\n\n"));
        if (function) {
            buffer.append(kco.format("    return null;\n\n"));
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
