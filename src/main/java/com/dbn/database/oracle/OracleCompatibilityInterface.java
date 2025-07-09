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

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Threads;
import com.dbn.common.util.Classes;
import com.dbn.common.util.Sockets;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionExceptionInfo;
import com.dbn.connection.ConnectionExceptionVisitor;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static com.dbn.connection.AuthenticationTokenType.OCI_INTERACTIVE;
import static com.dbn.database.DatabaseFeature.AI_ASSISTANT;
import static com.dbn.database.DatabaseFeature.AUTHID_METHOD_EXECUTION;
import static com.dbn.database.DatabaseFeature.CONNECTION_ERROR_RECOVERY;
import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.CURRENT_SCHEMA;
import static com.dbn.database.DatabaseFeature.DATABASE_LOGGING;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.EMBEDDED_JVM;
import static com.dbn.database.DatabaseFeature.EXPLAIN_PLAN;
import static com.dbn.database.DatabaseFeature.FUNCTION_OUT_ARGUMENTS;
import static com.dbn.database.DatabaseFeature.OBJECT_CHANGE_MONITORING;
import static com.dbn.database.DatabaseFeature.OBJECT_DDL_EXTRACTION;
import static com.dbn.database.DatabaseFeature.OBJECT_DEPENDENCIES;
import static com.dbn.database.DatabaseFeature.OBJECT_DISABLING;
import static com.dbn.database.DatabaseFeature.OBJECT_INVALIDATION;
import static com.dbn.database.DatabaseFeature.OBJECT_REPLACING;
import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.database.DatabaseFeature.READONLY_CONNECTIVITY;
import static com.dbn.database.DatabaseFeature.SESSION_BROWSING;
import static com.dbn.database.DatabaseFeature.SESSION_CURRENT_SQL;
import static com.dbn.database.DatabaseFeature.SESSION_DISCONNECT;
import static com.dbn.database.DatabaseFeature.SESSION_INTERRUPTION_TIMING;
import static com.dbn.database.DatabaseFeature.SESSION_KILL;
import static com.dbn.database.DatabaseFeature.UPDATABLE_RESULT_SETS;
import static com.dbn.database.DatabaseFeature.USER_SCHEMA;
import static com.dbn.database.DatabaseObjectTypeId.AI_PROFILE;
import static com.dbn.database.DatabaseObjectTypeId.CREDENTIAL;
import static com.dbn.database.DatabaseObjectTypeId.JAVA_CLASS;
import static com.dbn.database.DatabaseObjectTypeId.JAVA_RESOURCE;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public class OracleCompatibilityInterface extends DatabaseCompatibilityInterfaceImpl {
    public static final QuoteDefinition IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(new QuotePair('"', '"'));
    public static final int FAILURE_ON_PROVIDER_ERROR = 18726;

    @Override
    public boolean supportsObjectType(DatabaseObjectTypeId objectTypeId) {
        return true;
    }

    @Override
    public List<DatabaseObjectTypeId> getSupportedObjectTypes() {
        return Collections.emptyList(); // default implementation not used (all object types are supported)
    }

    @Override
    public List<DatabaseFeature> getSupportedFeatures() {
        return Arrays.asList(
                OBJECT_INVALIDATION,
                OBJECT_DEPENDENCIES,
                OBJECT_REPLACING,
                OBJECT_DDL_EXTRACTION,
                OBJECT_DISABLING,
                OBJECT_CHANGE_MONITORING,
                OBJECT_SOURCE_EDITING,
                AUTHID_METHOD_EXECUTION,
                FUNCTION_OUT_ARGUMENTS,
                DEBUGGING,
                EXPLAIN_PLAN,
                DATABASE_LOGGING,
                SESSION_BROWSING,
                SESSION_INTERRUPTION_TIMING,
                SESSION_DISCONNECT,
                SESSION_KILL,
                SESSION_CURRENT_SQL,
                CONNECTION_ERROR_RECOVERY,
                UPDATABLE_RESULT_SETS,
                CURRENT_SCHEMA,
                USER_SCHEMA,
                CONSTRAINT_MANIPULATION,
                READONLY_CONNECTIVITY,
                AI_ASSISTANT,
                EMBEDDED_JVM
                //EMPTY_SCHEMA_EVALUATION // TODO disabled due to performance reasons
                );
    }

    @Override
    public boolean supportsFeature(DatabaseFeature feature, DatabaseObjectTypeId objectTypeId) {
        if (!super.supportsFeature(feature, objectTypeId)) return false;

        if (feature == OBJECT_DDL_EXTRACTION) {
            // TODO create generic object-type to feature mapping solution
            return !objectTypeId.isOneOf(
                    CREDENTIAL,
                    AI_PROFILE,
                    JAVA_CLASS,
                    JAVA_RESOURCE);
        }
        return true;
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
    public SessionStatus getSessionStatus(String statusName) {
        try{
            return SessionStatus.valueOf(statusName);
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Invalid session status {}", statusName, e);
            return SessionStatus.INACTIVE;
        }
    }

    @Override
    public String getExplainPlanStatementPrefix() {
        return "explain plan for ";
    }

    @Override
    public String getDatabaseLogName() {
        return txt("app.logging.label.LogName_ORACLE");
    }

    @Override
    public Map<String, String> getImplicitConnectionProperties() {
        return Map.of(
                "oracle.jdbc.jsonDefaultGetObjectType", "java.lang.String",
                "oracle.jdbc.vectorDefaultGetObjectType", "double[]",
                "oracle.net.keepAlive", "true",
                "oracle.net.TCP_KEEPIDLE", "30",
                "oracle.net.TCP_KEEPINTERVAL", "30",
                "oracle.net.TCP_KEEPCOUNT", "5");
    }

    @Override
    public boolean handleConnectionException(final ConnectionExceptionInfo info) {
        ConnectionExceptionVisitor visitor = new ConnectionExceptionVisitor();
        info.accept(visitor);
        // if a bind exception was thrown or the error was due to an provider failure code
        if (visitor.hasBindException() || visitor.containsOraErrorCodes(FAILURE_ON_PROVIDER_ERROR)) {
            if (info.getAuthenticationInfo().getType() == AuthenticationType.TOKEN) {
                AuthenticationTokenType tokenType = info.getAuthenticationInfo().getTokenType();
                if (tokenType == OCI_INTERACTIVE) {
                    Future<?> future = Threads.backgroundExecutor().submit(new java.lang.Runnable() {
                        @Override
                        public void run() {
                            try {
                                // todo, use a backoff and retry?
                                if (!Sockets.tryToBindPort(8181)) {
                                    Sockets.pokeWebServer("http://localhost:8181/token?");
                                }
                            }
                            catch (final IOException ioe) {
                                Diagnostics.conditionallyLog(ioe);
                            }
                            try {
                                ClassLoader classLoader = info.getClassLoader();
                                Class<?> cacheControllerClass =
                                    Class.forName(
                                            "oracle.jdbc.provider.cache.CacheController", true, classLoader);
                                Method clearAllCaches = cacheControllerClass.getMethod("clearAllCaches");
                                clearAllCaches.invoke(null, new Object[0]);
                            } catch (final Exception e) {
                                Diagnostics.conditionallyLog(e);
                            }

                        }
                    });
                }
            }
        }
        return false;
    }
}