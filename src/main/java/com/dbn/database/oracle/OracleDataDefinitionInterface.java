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

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.toCsv;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.database.DatabaseObjectTypeId.DATABASE_TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.DATASET_TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.JAVA_CLASS;
import static com.dbn.database.DatabaseObjectTypeId.JSON_VIEW;
import static com.dbn.database.DatabaseObjectTypeId.MATERIALIZED_VIEW;
import static com.dbn.database.DatabaseObjectTypeId.TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.VIEW;
import static com.dbn.object.factory.model.DBObjectAttributeType.CONSTRAINT_COLUMNS;
import static com.dbn.object.factory.model.DBObjectAttributeType.CONSTRAINT_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.INDEX_COLUMNS;
import static com.dbn.object.factory.model.DBObjectAttributeType.INDEX_DEFINITION;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_INPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_NOT_NULL;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_OUTPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_PRIMARY_KEY;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;
import static com.dbn.object.type.DBObjectType.ARGUMENT;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.FUNCTION;

public class OracleDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public OracleDataDefinitionInterface(DatabaseInterfaces provider) {
        super("oracle_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();

        if (objectTypeId.isOneOf(DATABASE_TRIGGER, DATASET_TRIGGER)) {
            objectTypeId = TRIGGER;
        }

        if(objectTypeId == JAVA_CLASS){
            return kco.format("begin \n") +
                    kco.format("execute immediate \n") +
                    kco.format("' \n") +
                    kco.format("create" + (makeRerunnable ? " or replace" : "") + " and compile java source named " )
                    + "\"" + objectName.replace("/", ".") + "\""
                    + kco.format(" as\n") +
                    code +
                    "';\n" + "end;\n/";
        } else if (objectTypeId == VIEW) {
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " view ") + (useQualified ? schemaName + "." : "") + objectName + kco.format(" as\n") + code + "\n/";
        } else {
            String objectType = cachedLowerCase(objectTypeId.toString());
            if (contentType == DBContentType.CODE_BODY) {
                objectType = objectType + " body";
            }
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, styleCaseSettings);
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " ") + code + "\n/";
        }
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        String sourceCode = content.getText().toString();
        if (Strings.isEmpty(sourceCode)) return;

        if (objectTypeId.isOneOf(DATASET_TRIGGER, DATABASE_TRIGGER)) {
            if (!sourceCode.isEmpty()) {
                int startIndex = Strings.indexOfIgnoreCase(sourceCode, objectName, 0) + objectName.length();
                int headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "declare", startIndex);
                if (headerEndOffset == -1) headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "begin", startIndex);
                if (headerEndOffset == -1) headerEndOffset = Strings.indexOfIgnoreCase(sourceCode, "call", startIndex);
                if (headerEndOffset == -1) headerEndOffset = 0;
                content.getOffsets().setHeaderEndOffset(headerEndOffset);
            }
        }

        // view source-code does not contain the view name, hence exempted from guarded-block logic
        // TODO add custom guarded block logic for java classes (excluded for now)
        if (!objectTypeId.isOneOf(VIEW, JSON_VIEW, MATERIALIZED_VIEW, JAVA_CLASS)) {
            int nameIndex = Strings.indexOfIgnoreCase(sourceCode, objectName, 0);
            if (nameIndex > -1) {
                int guardedBlockEndOffset = nameIndex + objectName.length();
                if (guardedBlockEndOffset < sourceCode.length()) {
                    if (sourceCode.charAt(guardedBlockEndOffset) == '"'){
                        guardedBlockEndOffset++;
                    }
                    content.getOffsets().addGuardedBlock(0, guardedBlockEndOffset);
                }
            }
        }
    }


    @Override
    public String extractDDLStatement(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, "extract-ddl-statement", objectType, unquoted(ownerName), unquoted(objectName));
            resultSet.next();
            Clob clob = resultSet.getClob(1);
            return Resources.readClob(clob);
        } finally {
            Resources.close(resultSet);
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-view", ownerName, viewName, code, editionable ? "editionable" : "noneditionable");
    }

    @Override
    public void updateJsonView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "change-json-view", ownerName, viewName, code, editionable ? "editionable" : "noneditionable");
    }

    @Override
    public void updateTrigger(String ownerName, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        updateObject(ownerName, triggerName, "trigger", oldCode, newCode, connection);
    }

    @Override
    public void updateObject(String ownerName, String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        // code assumed to contain object type and name
        executeUpdate(connection, "update-object", newCode);
    }


    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(@NotNull DBObjectSpec methodSpec, DBNConnection connection) throws SQLException {
        // TODO SQL-Injection
        Project project = methodSpec.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dco = styleCaseSettings.getDatatypeCaseOption();
        boolean function = methodSpec.getObjectType() == FUNCTION;

        StringBuilder buffer = new StringBuilder();
        String methodType = function ? "function " : "procedure ";
        buffer.append(kco.format(methodType));
        buffer.append(oco.format(methodSpec.getObjectName()));
        buffer.append("(");
        
        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        DBObjectSpecList<DBObjectSpec> arguments = methodSpec.getChildren(ARGUMENT);
        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength, in && out ? 6 : in ? 2 : out ? 3 : 0);
        }

        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);

            buffer.append("\n    ");
            buffer.append(oco.format(argument.getObjectName()));
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));
            String direction =
                    in && out ? kco.format("in out") :
                    in ? kco.format("in") :
                    out ? kco.format("out") : "";
            buffer.append(direction);
            buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            buffer.append(dco.format(DATA_TYPE.of(argument)));
            if (argument != Lists.lastElement(arguments)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (function) {
            DBObjectSpec returnArgument = RETURN_ARGUMENT.of(methodSpec);
            buffer.append(kco.format("return "));
            buffer.append(dco.format(DATA_TYPE.of(returnArgument)));
            buffer.append("\n");
        }
        buffer.append(kco.format("is\nbegin\n\n"));
        if (function) buffer.append(kco.format("    return null;\n\n"));
        buffer.append("end;");
        createObject(buffer.toString(), connection);
    }

    @Override
    public void createTable(DBObjectSpec tableSpec, DBNConnection connection) throws SQLException {
        StringBuilder builder = new StringBuilder();
        builder.append("table ");
        builder.append(tableSpec.getSchemaName(true));
        builder.append(".");
        builder.append(tableSpec.getObjectName(true));
        builder.append(" (\n");

        boolean first = true;
        DBObjectSpecList<DBObjectSpec> columnSpecs = tableSpec.getChildren(COLUMN);
        for (DBObjectSpec columnSpec : columnSpecs) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }
            builder.append("    ");
            builder.append(columnSpec.getObjectName(true));
            builder.append(" ");
            builder.append(DATA_TYPE.of(columnSpec));
            builder.append(IS_NOT_NULL.is(columnSpec) ? " not null" : "");
            builder.append(IS_PRIMARY_KEY.is(columnSpec) ? " primary key" : "");
        }

        DBObjectSpecList<DBObjectSpec> constraintSpecs = tableSpec.getChildren(CONSTRAINT);
        for (DBObjectSpec constraintSpec : constraintSpecs) {
            String constraintType = CONSTRAINT_TYPE.of(constraintSpec);
            String[] constraintColumns = CONSTRAINT_COLUMNS.of(constraintSpec);

            builder.append(",\n");
            builder.append("    ");
            builder.append(constraintType);
            builder.append(" ");
            builder.append(nvl(constraintSpec.getObjectName(), ""));
            builder.append("(");
            builder.append(toCsv(Arrays.asList(constraintColumns), s -> s));
            builder.append(")");
        }

        builder.append(")\n");
        builder.append(nvl(OBJECT_DETAIL.of(tableSpec), ""));

        createObject(builder.toString(), connection);
    }

    @Override
    public void createIndex(DBObjectSpec indexSpec, DBNConnection connection) throws SQLException {
        DBObjectSpec tableSpec = indexSpec.getParent();
        String schemaName = tableSpec.getSchemaName(true);
        String indexName = indexSpec.getObjectName(true);
        String tableName = tableSpec.getObjectName(true);

        StringBuilder builder = new StringBuilder();
        builder.append("index ");

        builder.append(schemaName);
        builder.append(".");
        builder.append(indexName);
        builder.append("\n");

        builder.append("on ");
        builder.append(schemaName);
        builder.append(".");
        builder.append(tableName);
        builder.append("\n(");

        String indexDefinition = INDEX_DEFINITION.of(indexSpec);
        String[] indexColumns = INDEX_COLUMNS.of(indexSpec);
        if (Strings.isNotEmpty(indexDefinition)) {
            builder.append(indexDefinition);
        } else if (indexColumns != null) {
            builder.append(toCsv(Arrays.asList(indexColumns), s -> s));
        }

        builder.append(")\n");

        createObject(builder.toString(), connection);
    }
}
