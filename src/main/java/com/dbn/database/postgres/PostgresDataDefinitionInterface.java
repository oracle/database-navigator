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
import com.dbn.object.factory.ObjectFactoryIdentifiers;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.notImplemented;
import static com.dbn.database.DatabaseObjectTypeId.DATABASE_TRIGGER;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_INPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.IS_OUTPUT;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CACHE_SIZE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CYCLE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_INCREMENT_BY;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MAX_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MIN_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_START_WITH;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FOR_EACH_ROW;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FUNCTION_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBObjectType.ARGUMENT;

public class PostgresDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public PostgresDataDefinitionInterface(DatabaseInterfaces provider) {
        super("postgres_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        schemaName = quoted(schemaName);
        objectName = quoted(objectName);

        return objectTypeId == DatabaseObjectTypeId.VIEW ? "create view " + objectName + " as\n" + code :
                objectTypeId == DatabaseObjectTypeId.MATERIALIZED_VIEW ? "create materialized view " + objectName + " as\n" + code :
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
        return notImplemented();
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
        if (DBObjectType.DATABASE_TRIGGER.getName().equalsIgnoreCase(objectType)) {
            updateEventTrigger(objectName, oldCode, newCode, connection);
            return;
        }

        executeUpdate(connection, "update-object", newCode);
    }

    private void updateEventTrigger(String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        executeStatement(connection, "drop-event-trigger", triggerName);
        try {
            executeStatement(connection, "update-event-trigger", newCode);
        } catch (SQLException e) {
            conditionallyLog(e);
            try {
                executeStatement(connection, "update-event-trigger", oldCode);
            } catch (SQLException restoreException) {
                conditionallyLog(restoreException);
            }
            throw e;
        }
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
    public void createTrigger(DBObjectSpec triggerSpec, DBNConnection connection) throws SQLException {
        if (triggerSpec.getObjectTypeId() == DATABASE_TRIGGER) {
            createEventTrigger(triggerSpec, connection);
            return;
        }

        DBTriggerEvent[] triggerEvents = TRIGGER_EVENTS.values(triggerSpec);

        String functionName = TRIGGER_FUNCTION_NAME.value(triggerSpec);
        if (Strings.isEmptyOrSpaces(functionName)) {
            functionName = triggerSpec.getObjectName();
        }
        String triggerFunctionName = triggerSpec.getSchemaName(true) + '.' + ObjectFactoryIdentifiers.quoteIdentifier(
                triggerSpec.getConnection(), functionName.trim());
        executeUpdate(connection, "create-trigger-function", triggerFunctionName, TRIGGER_BODY.value(triggerSpec));

        @NonNls
        StringBuilder builder = new StringBuilder("trigger ");
        builder.append(triggerSpec.getAdjustedObjectName());
        builder.append('\n');
        builder.append(TRIGGER_TYPE.value(triggerSpec).getName());
        builder.append(' ');

        for (int i = 0; i < triggerEvents.length; i++) {
            if (i > 0) builder.append(" or ");
            builder.append(triggerEvents[i].getName());
        }

        builder.append(" on ");
        builder.append(triggerSpec.getSchemaName(true));
        builder.append('.');
        builder.append(ObjectFactoryIdentifiers.quoteIdentifier(
                triggerSpec.getConnection(),
                TRIGGER_TARGET_DATASET.value(triggerSpec)));
        builder.append(TRIGGER_FOR_EACH_ROW.is(triggerSpec) ? "\nfor each row\n" : "\nfor each statement\n");
        builder.append("execute function ").append(triggerFunctionName).append("()");

        try {
            createObject(builder.toString(), connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            try {
                executeUpdate(connection, "drop-trigger-function", triggerFunctionName);
            } catch (SQLException cleanupException) {
                conditionallyLog(cleanupException);
            }
            throw e;
        }
    }

    private void createEventTrigger(DBObjectSpec triggerSpec, DBNConnection connection) throws SQLException {
        DBTriggerEvent[] triggerEvents = TRIGGER_EVENTS.values(triggerSpec);
        DBTriggerEvent triggerEvent = triggerEvents.length == 0 ? null : triggerEvents[0];
        if (triggerEvent == null) {
            throw new SQLException("PostgreSQL event trigger event is not specified");
        }
        String eventName = switch (triggerEvent) {
            case DDL -> "ddl_command_start";
            case DROP -> "sql_drop";
            case LOGON -> "login";
            default -> throw new SQLException("Unsupported PostgreSQL event trigger event: " + triggerEvent);
        };

        String functionName = TRIGGER_FUNCTION_NAME.value(triggerSpec);
        if (Strings.isEmptyOrSpaces(functionName)) {
            functionName = triggerSpec.getObjectName();
        }
        String triggerFunctionName = triggerSpec.getSchemaName(true) + '.' + ObjectFactoryIdentifiers.quoteIdentifier(
                triggerSpec.getConnection(), functionName.trim());
        executeUpdate(connection, "create-event-trigger-function", triggerFunctionName, TRIGGER_BODY.value(triggerSpec));

        @NonNls
        StringBuilder builder = new StringBuilder("event trigger ")
                .append(triggerSpec.getAdjustedObjectName())
                .append(" on ")
                .append(eventName)
                .append(" execute function ")
                .append(triggerFunctionName)
                .append("()");

        try {
            createObject(builder.toString(), connection);
        } catch (SQLException e) {
            conditionallyLog(e);
            try {
                executeUpdate(connection, "drop-event-trigger-function", triggerFunctionName);
            } catch (SQLException cleanupException) {
                conditionallyLog(cleanupException);
            }
            throw e;
        }
    }

    @Override
    public void createSequence(DBObjectSpec sequenceSpec, DBNConnection connection) throws SQLException {
        @NonNls
        StringBuilder builder = new StringBuilder("sequence ")
                .append(sequenceSpec.getSchemaName(true))
                .append('.')
                .append(sequenceSpec.getAdjustedObjectName());

        appendOption(builder, " increment by ", SEQUENCE_INCREMENT_BY.value(sequenceSpec));
        appendOption(builder, " minvalue ", SEQUENCE_MIN_VALUE.value(sequenceSpec));
        appendOption(builder, " maxvalue ", SEQUENCE_MAX_VALUE.value(sequenceSpec));
        appendOption(builder, " start with ", SEQUENCE_START_WITH.value(sequenceSpec));
        appendOption(builder, " cache ", SEQUENCE_CACHE_SIZE.value(sequenceSpec));
        if (SEQUENCE_CYCLE.is(sequenceSpec)) builder.append(" cycle");

        createObject(builder.toString(), connection);
    }

    @Override
    public void createMethod(@NotNull DBObjectSpec methodSpec, DBNConnection connection) throws SQLException {
        Project project = methodSpec.getSchema().getProject();
        CodeStyleCaseSettings styleCaseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption dco = styleCaseSettings.getDatatypeCaseOption();
        boolean function = methodSpec.getObjectType() == DBObjectType.FUNCTION;

        @NonNls
        StringBuilder buffer = new StringBuilder();
        String methodType = function ? "function " : "procedure ";
        buffer.append(kco.format(methodType));
        buffer.append(methodSpec.getAdjustedObjectName());
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        DBObjectSpecList arguments = methodSpec.getChildren(ARGUMENT);
        for (DBObjectSpec argument : arguments) {
            boolean in = IS_INPUT.is(argument);
            boolean out = IS_OUTPUT.is(argument);
            String argumentName = argument.getAdjustedObjectName();
            maxArgNameLength = Math.max(maxArgNameLength, argumentName.length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength, in && out ? 5 : in ? 2 : out ? 3 : 0);
        }


        for (DBObjectSpec argumentSpec : arguments) {
            boolean in = IS_INPUT.is(argumentSpec);
            boolean out = IS_OUTPUT.is(argumentSpec);

            buffer.append("\n    ");
            if (!function) {
                String direction =
                        in && out ? kco.format("inout") :
                        in ? kco.format("in") :
                        out ? kco.format("out") : "";
                buffer.append(direction);
                buffer.append(Strings.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            String argumentName = argumentSpec.getAdjustedObjectName();
            buffer.append(argumentName);
            buffer.append(Strings.repeatSymbol(' ', maxArgNameLength - argumentName.length() + 1));

            String dataType = DATA_TYPE.value(argumentSpec);
            buffer.append(dco.format(dataType));
            if (argumentSpec != Lists.lastElement(arguments)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (function) {
            DBObjectSpec returnArgument = RETURN_ARGUMENT.value(methodSpec);
            buffer.append(kco.format("returns "));
            buffer.append(dco.format(DATA_TYPE.value(returnArgument)));
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
