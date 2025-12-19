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
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.model.DBArgumentFactoryInput;
import com.dbn.object.factory.model.DBMethodFactoryInput;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.common.util.Naming.unquote;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.database.DatabaseObjectTypeId.DATABASE_TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.DATASET_TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.JAVA_CLASS;
import static com.dbn.database.DatabaseObjectTypeId.JSON_VIEW;
import static com.dbn.database.DatabaseObjectTypeId.MATERIALIZED_VIEW;
import static com.dbn.database.DatabaseObjectTypeId.TRIGGER;
import static com.dbn.database.DatabaseObjectTypeId.VIEW;

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
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        if (objectTypeId.isOneOf(DATABASE_TRIGGER, DATASET_TRIGGER)) {
            objectTypeId = TRIGGER;
        }

        if(objectTypeId == JAVA_CLASS){
            return kco.format("begin \n") +
                    kco.format("execute immediate \n") +
                    kco.format("' \n") +
                    kco.format("create" + (makeRerunnable ? " or replace" : "") + " and compile java source named " )
                    + "\"" + oco.format(objectName.replace("/", ".")) + "\""
                    + kco.format(" as\n") +
                    code +
                    "';\n" + "end;\n/";
        } else if (objectTypeId == VIEW) {
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " view ") + oco.format((useQualified ? schemaName + "." : "") + objectName) + kco.format(" as\n") + code + "\n/";
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

    public void createJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "create-java-source", ownerName, objectName, content);
        compileJavaClass(ownerName, objectName, connection);
    }

    public void compileJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        try {
            executeSilentUpdate(connection, "set-java-property", "sun.tools.javac.Main.args", 'g');
            executeSilentUpdate(connection, "set-java-compiler-option", unquote(objectName), "debug", "true");
            executeUpdate(connection, "compile-java-class", ownerName, objectName);
        } finally {
            executeSilentUpdate(connection, "set-java-compiler-option", unquote(objectName), "debug", "false");
        }
    }

    @Override
    public void updateJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "update-java-source", ownerName, objectName, content);
        compileJavaClass(ownerName, objectName, connection);
    }

    @Override
    public void replaceJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "drop-java-object", ownerName, objectName);
        executeUpdate(connection, "create-java-source", ownerName, objectName, content);
    }

    @Override
    public void replaceJavaClass(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "drop-java-object", ownerName, objectName);
        executeUpdate(connection, "create-java-class", ownerName, objectName, content);
    }

    @Override
    public void updateJavaResource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "update-java-resource", ownerName, objectName, content);
    }


    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(DBMethodFactoryInput method, DBNConnection connection) throws SQLException {
        // TODO SQL-Injection
        Project project = method.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();
        CodeStyleCaseOption dco = styleCaseSettings.getDatatypeCaseOption();

        StringBuilder buffer = new StringBuilder();
        String methodType = method.isFunction() ? "function " : "procedure ";
        buffer.append(kco.format(methodType));
        buffer.append(oco.format(method.getObjectName()));
        buffer.append("(");
        
        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        for (DBArgumentFactoryInput argument : method.getArguments()) {
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength,
                    argument.isInput() && argument.isOutput() ? 6 :
                    argument.isInput() ? 2 :
                    argument.isOutput() ? 3 : 0);
        }


        for (DBArgumentFactoryInput argument : method.getArguments()) {
            buffer.append("\n    ");
            buffer.append(oco.format(argument.getObjectName()));
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));
            String direction =
                    argument.isInput() && argument.isOutput() ? kco.format("in out") :
                    argument.isInput() ? kco.format("in") :
                    argument.isOutput() ? kco.format("out") : "";
            buffer.append(direction);
            buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            buffer.append(dco.format(argument.getDataType()));
            if (argument != method.getArguments().get(method.getArguments().size() -1)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (method.isFunction()) {
            buffer.append(kco.format("return "));
            buffer.append(dco.format(method.getReturnArgument().getDataType()));
            buffer.append("\n");
        }
        buffer.append(kco.format("is\nbegin\n\n"));
        if (method.isFunction()) buffer.append(kco.format("    return null;\n\n"));
        buffer.append("end;");
        createObject(buffer.toString(), connection);
    }
}