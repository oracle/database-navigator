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

import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectorProperties;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.quotes.QuoteDefinition;
import com.dbn.language.common.quotes.QuotePair;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.factory.model.DBObjectTypeSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static com.dbn.database.DatabaseFeature.CHANGE_PASSWORD;
import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.CURRENT_SCHEMA;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.database.DatabaseFeature.SESSION_BROWSING;
import static com.dbn.database.DatabaseFeature.SESSION_CURRENT_SQL;
import static com.dbn.database.DatabaseFeature.SESSION_KILL;
import static com.dbn.database.DatabaseFeature.TRANSACTIONAL_DDL;
import static com.dbn.database.DatabaseFeature.UPDATABLE_RESULT_SETS;
import static com.dbn.database.DatabaseObjectTypeId.MATERIALIZED_VIEW;
import static com.dbn.database.DatabaseObjectTypeId.USER;
import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.ENABLE;
import static com.dbn.object.event.ObjectChangeAction.REFRESH;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY_TEMPLATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FOR_EACH_ROW;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FUNCTION_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBTriggerEvent.DDL;
import static com.dbn.object.type.DBTriggerEvent.DELETE;
import static com.dbn.object.type.DBTriggerEvent.DROP;
import static com.dbn.object.type.DBTriggerEvent.INSERT;
import static com.dbn.object.type.DBTriggerEvent.TRUNCATE;
import static com.dbn.object.type.DBTriggerEvent.UPDATE;
import static com.dbn.object.type.DBTriggerTarget.DATABASE;
import static com.dbn.object.type.DBTriggerType.AFTER;
import static com.dbn.object.type.DBTriggerType.BEFORE;
import static com.dbn.object.type.DBTriggerType.INSTEAD_OF;

public class PostgresCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {

    public static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('"', '"'));

    @Override
    public List<DatabaseObjectTypeId> getSupportedObjectTypes() {
        return Arrays.asList(
                DatabaseObjectTypeId.CONSOLE,
                DatabaseObjectTypeId.CHARSET,
                DatabaseObjectTypeId.USER,
                DatabaseObjectTypeId.SCHEMA,
                DatabaseObjectTypeId.TABLE,
                DatabaseObjectTypeId.VIEW,
                DatabaseObjectTypeId.MATERIALIZED_VIEW,
                DatabaseObjectTypeId.COLUMN,
                DatabaseObjectTypeId.CONSTRAINT,
                DatabaseObjectTypeId.INDEX,
                DatabaseObjectTypeId.DATASET_TRIGGER,
                DatabaseObjectTypeId.DATABASE_TRIGGER,
                DatabaseObjectTypeId.FUNCTION,
                DatabaseObjectTypeId.ARGUMENT,
                DatabaseObjectTypeId.SEQUENCE,
                DatabaseObjectTypeId.SYSTEM_PRIVILEGE,
                DatabaseObjectTypeId.GRANTED_PRIVILEGE);
    }

    @Override
    public DBObjectTypeSpec getObjectTypeSpec(DatabaseObjectTypeId objectTypeId) {
        return switch (objectTypeId) {
            case DATASET_TRIGGER ->
                    DBObjectTypeSpec.create(objectTypeId)
                            .withAttribute(TRIGGER_TYPE, List.of(BEFORE, AFTER, INSTEAD_OF), false, true)
                            .withAttribute(TRIGGER_EVENTS, List.of(INSERT, UPDATE, DELETE, TRUNCATE), true, true)
                            .withAttribute(TRIGGER_FUNCTION_NAME, List.of(), false, true)
                            .withAttribute(TRIGGER_TARGET_DATASET, List.of(), false, true)
                            .withAttribute(TRIGGER_FOR_EACH_ROW, List.of(false, true), false, false)
                            .withAttribute(TRIGGER_BODY_TEMPLATE, "begin\n    return new;\nend;", false)
                            .withAttribute(TRIGGER_BODY, List.of(), false, true);
            case DATABASE_TRIGGER ->
                    DBObjectTypeSpec.create(objectTypeId)
                            .withAttribute(TRIGGER_EVENTS, List.of(DDL, DROP), false, true)
                            .withAttribute(TRIGGER_FUNCTION_NAME, List.of(), false, true)
                            .withAttribute(TRIGGER_TARGET, DATABASE, true)
                            .withAttribute(TRIGGER_BODY_TEMPLATE, "begin\n\nend;", false)
                            .withAttribute(TRIGGER_BODY, List.of(), false, true);
            //...
            default -> super.getObjectTypeSpec(objectTypeId);
        };
    }

    @Override
    public boolean supportsObjectType(DatabaseObjectTypeId objectTypeId, double databaseVersion) {
        return switch (objectTypeId) {
            case MATERIALIZED_VIEW -> databaseVersion >= 9.3;
            case DATABASE_TRIGGER -> databaseVersion >= 9.3;
            default -> supportsObjectType(objectTypeId);
        };
    }

    @Override
    public boolean supportsObjectAction(DatabaseObjectTypeId objectTypeId, ObjectChangeAction action) {
        if (objectTypeId == USER && action.isOneOf(ENABLE, DISABLE)) return true;
        if (objectTypeId == MATERIALIZED_VIEW && action == REFRESH) return true;

        return super.supportsObjectAction(objectTypeId, action);
    }

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                SESSION_BROWSING,
                SESSION_KILL,
                SESSION_CURRENT_SQL,
                UPDATABLE_RESULT_SETS,
                OBJECT_SOURCE_EDITING,
                TRANSACTIONAL_DDL,
                CURRENT_SCHEMA,
                CHANGE_PASSWORD,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY);
    }

    @Override
    public SessionStatus getSessionStatus(String statusName) {
        if (Strings.isEmpty(statusName)) return SessionStatus.INACTIVE;
        if (statusName.equalsIgnoreCase("active")) return SessionStatus.ACTIVE;
        if (statusName.equalsIgnoreCase("idle")) return SessionStatus.INACTIVE;
        return SessionStatus.SNIPED;
    }

    @Override
    public QuoteDefinition getIdentifierQuotes() {
        return IDENTIFIER_QUOTE_DEFINITION;
    }

    @Override
    public String getDefaultAlternativeStatementDelimiter() {
        return null;
    }

    @Override
    public void completeConnectorPasswordChange(@NotNull Connection connection, @Nullable char[] newPassword) throws SQLException {
        if (newPassword == null) return;
        try (Statement statement = connection.createStatement()) {
            String password = Chars.toStringAcceptEmpty(newPassword);
            String passwordLiteral = statement.enquoteLiteral(password);
            statement.execute("ALTER ROLE CURRENT_USER PASSWORD " + passwordLiteral);
        }
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return "explain analyze verbose ";
    }

    @Override
    public void initConnectorSslConnection(ConnectorProperties properties, ConnectionSettings settings) {
        ConnectionSslSettings sslSettings = settings.getSslSettings();
        if (!sslSettings.isActive()) return;

        super.initConnectorSslConnection(properties, settings);
        properties.add("sslmode", "verify-full");
    }
}
