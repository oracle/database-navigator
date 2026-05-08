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

package com.dbn.database.interfaces;

import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.connection.ConnectionExceptionInfo;
import com.dbn.connection.ConnectorProperties;
import com.dbn.connection.DatabaseAttachmentHandler;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.data.sorting.SortDirection;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.JdbcProperty;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.dbn.assistant.tool.AssistantToolType.JAVA_METADATA;
import static com.dbn.assistant.tool.AssistantToolType.JAVA_SOURCE_CODE;
import static com.dbn.assistant.tool.AssistantToolType.JAVA_SOURCE_CODE_EDITORS;
import static com.dbn.assistant.tool.AssistantToolType.SEMANTIC_SEARCH;
import static com.dbn.database.DatabaseFeature.JAVA_VIRTUAL_MACHINE;
import static com.dbn.database.DatabaseFeature.VECTOR_SEARCH;

public interface DatabaseCompatibilityInterface extends DatabaseInterface {
    List<DatabaseObjectTypeId> getSupportedObjectTypes();

    List<DatabaseFeature> getSupportedFeatures();

    boolean supportsObjectType(DatabaseObjectTypeId objectTypeId);

    boolean supportsFeature(DatabaseFeature feature);

    boolean supportsFeature(DatabaseFeature feature, DatabaseObjectTypeId objectTypeId);

    boolean supportsOperation(DatabaseOperation operation);

    QuoteDefinition getIdentifierQuotes();

    QuotePair getDefaultIdentifierQuotes();

    @NonNls
    @Nullable
    String getDatabaseLogName();

    @NonNls
    String getDefaultAlternativeStatementDelimiter();

    @NonNls
    String getOrderByClause(String columnName, SortDirection sortDirection, boolean nullsFirst);

    @NonNls
    String getForUpdateClause();

    @NonNls
    String getSessionBrowserColumnName(String columnName);

    SessionStatus getSessionStatus(String statusName);

    @NonNls
    String getExplainPlanStatementPrefix();

    @Nullable
    DatabaseAttachmentHandler getDatabaseAttachmentHandler();

    <T> T attemptFeatureInvocation(JdbcProperty feature, Callable<T> invoker) throws SQLException;

    default boolean useMetadataIdentifierQuoting() {
        return false;
    }

    default Map<String, String> getImplicitConnectionProperties() {
        return Map.of();
    }

    /**
     * Does database-specific handling of a connection error
     * @param info
     * @return true if throwable was handled, false if it was ignored by the specific database compatibility
     * implementation.
     */
    default boolean handleConnectionException(final ConnectionExceptionInfo info) {
        return false;
    }


    ConnectorProperties createConnectorProperties();

    void initConnectorAuthentication(ConnectorProperties properties, AuthenticationInfo authenticationInfo);

    void initConnectorSession(ConnectorProperties properties, ConnectionSettings settings, SessionId sessionId);

    void initConnectorDebugger(ConnectorProperties properties, ConnectionSettings settings);

    void initConnectorSslConnection(ConnectorProperties properties, ConnectionSettings settings);

    void initConnectorFileAttachments(ConnectionSettings settings, Connection connection);

    boolean resetConnectorAndRetry(Throwable e, ConnectionSettings settings);

    default boolean isAssistantToolSupported(AssistantToolCategory toolCategory) {
        return true;
    }

    default boolean isAssistantToolSupported(AssistantToolType toolType) {
        if (toolType == SEMANTIC_SEARCH) {
            return supportsFeature(VECTOR_SEARCH);
        }

        if (toolType.isOneOf(
                JAVA_METADATA,
                JAVA_SOURCE_CODE,
                JAVA_SOURCE_CODE_EDITORS)) {
            return supportsFeature(JAVA_VIRTUAL_MACHINE);
        }

        return true;
    }
}
