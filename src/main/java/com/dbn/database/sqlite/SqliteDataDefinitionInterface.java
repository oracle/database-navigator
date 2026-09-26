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
import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.Strings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.factory.ObjectFactoryIdentifiers;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBTriggerEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.notImplemented;
import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;

public class SqliteDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    SqliteDataDefinitionInterface(DatabaseInterfaces provider) {
        super("sqlite_ddl_interface.xml", provider);
    }


    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        schemaName = quoted(schemaName);
        objectName = quoted(objectName);

        if (Strings.isEmpty(alternativeDelimiter)) {
            alternativeDelimiter = getInterfaces().getCompatibilityInterface().getDefaultAlternativeStatementDelimiter();
        }

        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings caseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();


        if (objectTypeId.isOneOf(DatabaseObjectTypeId.VIEW, DatabaseObjectTypeId.DATASET_TRIGGER)) {
            if (objectTypeId == DatabaseObjectTypeId.DATASET_TRIGGER) {
                objectTypeId = DatabaseObjectTypeId.TRIGGER;
            }
            String objectType = cachedLowerCase(objectTypeId.toString());
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, caseSettings);
            String dropStatement =
                    kco.format("drop " + objectType + " if exists ") +
                    (useQualified ? schemaName + "." : "") + objectName + alternativeDelimiter + "\n";
            String createStatement = kco.format("create \n") + code + alternativeDelimiter + "\n";
            return (makeRerunnable ? dropStatement : "") + createStatement;
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        super.computeSourceCodeOffsets(content, objectTypeId, objectName);
    }


    @Override
    public String extractDDLStatement(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException {
        return notImplemented();
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void createTrigger(DBObjectSpec triggerSpec, DBNConnection connection) throws SQLException {
        DBTriggerEvent[] triggerEvents = TRIGGER_EVENTS.of(triggerSpec);
        DBTriggerEvent triggerEvent = triggerEvents == null || triggerEvents.length == 0 ? null : triggerEvents[0];

        @NonNls
        StringBuilder builder = new StringBuilder("trigger ");
        builder.append(triggerSpec.getSchemaName(true));
        builder.append('.');
        builder.append(triggerSpec.getAdjustedObjectName());
        builder.append('\n');
        builder.append(TRIGGER_TYPE.of(triggerSpec).getName());
        builder.append(' ');
        builder.append(triggerEvent.getName());
        builder.append(" on ");
        builder.append(ObjectFactoryIdentifiers.quoteIdentifier(
                triggerSpec.getConnection(),
                TRIGGER_TARGET_DATASET.of(triggerSpec)));
        builder.append("\nfor each row\n");
        builder.append(OBJECT_DETAIL.of(triggerSpec));

        createObject(builder.toString(), connection);
    }

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
}
