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

package com.dbn.database.sqlite;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.model.DBMethodSpec;
import com.dbn.object.factory.model.DBTableSpec;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class SqliteDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    SqliteDataDefinitionInterface(DatabaseInterfaces provider) {
        super("sqlite_ddl_interface.xml", provider);
    }


    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        if (Strings.isEmpty(alternativeDelimiter)) {
            alternativeDelimiter = getInterfaces().getCompatibilityInterface().getDefaultAlternativeStatementDelimiter();
        }

        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings caseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = caseSettings.getObjectCaseOption();


        if (objectTypeId.isOneOf(DatabaseObjectTypeId.VIEW, DatabaseObjectTypeId.DATASET_TRIGGER)) {
            if (objectTypeId == DatabaseObjectTypeId.DATASET_TRIGGER) {
                objectTypeId = DatabaseObjectTypeId.TRIGGER;
            }
            String objectType = cachedLowerCase(objectTypeId.toString());
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, caseSettings);
            String dropStatement =
                    kco.format("drop " + objectType + " if exists ") +
                    oco.format((useQualified ? schemaName + "." : "") + objectName) + alternativeDelimiter + "\n";
            String createStatement = kco.format("create \n") + code + alternativeDelimiter + "\n";
            return (makeRerunnable ? dropStatement : "") + createStatement;
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        super.computeSourceCodeOffsets(content, objectTypeId, objectName);
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException {
        // try instructions
        String objectType = "VIEW";
        String tempViewName = getTempObjectName(objectType);
        dropObjectIfExists(objectType, ownerName, tempViewName, connection);
        createView(tempViewName, code, connection);
        dropObjectIfExists(objectType, ownerName, tempViewName, connection);

        // instructions
        dropObjectIfExists(objectType, ownerName, viewName, connection);
        createView(viewName, code, connection);
    }

    @Override
    public void updateTrigger(String ownerName, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        String objectType = "TRIGGER";
        String tempTriggerName = getTempObjectName(objectType);
        dropObjectIfExists(objectType, ownerName, tempTriggerName, connection);
        createObject(newCode.replaceFirst("(?i)" + triggerName, tempTriggerName), connection);
        dropObjectIfExists(objectType, ownerName, tempTriggerName, connection);

        dropObjectIfExists(objectType, ownerName, triggerName, connection);
        createObject(newCode, connection);
    }

    @Override
    public void updateObject(String ownerName, String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        dropObjectIfExists(objectType, ownerName, objectName, connection);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            createObject(oldCode, connection);
            throw e;
        }
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(DBMethodSpec method, DBNConnection connection) throws SQLException {
        throw new SQLException("Operation not supported: [create method]");
    }

    @Override
    public void createTable(DBTableSpec spec, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
