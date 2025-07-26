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

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Sockets;
import com.dbn.common.util.Strings;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionExceptionInfo;
import com.dbn.connection.ConnectionExceptionVisitor;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.ConnectorProperties;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseCompatibilityInterfaceImpl;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.editor.session.SessionStatus;
import com.dbn.language.common.QuoteDefinition;
import com.dbn.language.common.QuotePair;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.connection.AuthenticationTokenType.AZURE_INTERACTIVE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_TOKEN;
import static com.dbn.connection.AuthenticationTokenType.OCI_API_KEY;
import static com.dbn.connection.AuthenticationTokenType.OCI_INTERACTIVE;
import static com.dbn.database.DatabaseFeature.AI_ASSISTANT;
import static com.dbn.database.DatabaseFeature.AUTHID_METHOD_EXECUTION;
import static com.dbn.database.DatabaseFeature.CONNECTION_ERROR_RECOVERY;
import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.database.DatabaseFeature.CURRENT_SCHEMA;
import static com.dbn.database.DatabaseFeature.DATABASE_LOGGING;
import static com.dbn.database.DatabaseFeature.DATA_CHANGE_NOTIFICATION;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.EXPLAIN_PLAN;
import static com.dbn.database.DatabaseFeature.FUNCTION_OUT_ARGUMENTS;
import static com.dbn.database.DatabaseFeature.JAVA_VIRTUAL_MACHINE;
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

    private interface Property {
        String SESSION_PROGRAM = "v$session.program";

        String ORACLE_JDBC_OCI_PROFILE = "oracle.jdbc.ociProfile";
        String ORACLE_JDBC_OCI_CONFIG_FILE = "oracle.jdbc.ociConfigFile";
        String ORACLE_JDBC_OCI_COMPARTMENT = "oracle.jdbc.ociCompartment";
        String ORACLE_JDBC_OCI_DATABASE = "oracle.jdbc.ociDatabase";
        // if this value is set, it puts the driver into on of the token auth
        // modes based setting one of PropertyValue.TOKEN_AUTHENTICATION_*
        String ORACLE_JDBC_TOKEN_AUTHENTICATION = "oracle.jdbc.tokenAuthentication";
        String ORACLE_JDBC_DEBUG_JDWP = "oracle.jdbc.debugJDWP";
        String ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI = "oracle.jdbc.azureDatabaseApplicationIdUri";
        String ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_FILE = "oracle.jdbc.clientCertificate";
        String ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_PASSWORD = "oracle.jdbc.clientCertificatePassword";
        String ORACLE_JDBC_AZURE_CLIENT_SECRET = "oracle.jdbc.clientSecret";
        String ORACLE_JDBC_AZURE_CLIENT_ID = "oracle.jdbc.clientId";
        String ORACLE_JDBC_AZURE_TENANT_ID = "oracle.jdbc.tenantId";
        String ORACLE_JDBC_SSL_SERVER_DN_MATCH = "oracle.net.ssl_server_dn_match";
    }

    @NonNls
    private interface PropertyValue {
        String TOKEN_AUTHENTICATION_OCI_API_KEY = "OCI_API_KEY";
        String TOKEN_AUTHENTICATION_OCI_INTERACTIVE = "OCI_INTERACTIVE";
        String TOKEN_AUTHENTICATION_AZURE_INTERACTIVE = "AZURE_INTERACTIVE";
        String TOKEN_AUTHENTICATION_AZURE_SERVICE_PRINCIPAL = "AZURE_SERVICE_PRINCIPAL";
    }

    /**
     * Encapsulates constants used in handling connection errors related to issues
     * in the Oracle JDBC driver "provider" framework
     */
    public static class ProviderErrorHandlingConstants {
        /**
         * The TCP port that the OCI_INTERACTIVE token provider needs to bind
         * in order to receive a response from the browser.
         */
        public static final int OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT = 8181;
        /**
         * The ORA error code we look for to indicate that a connection attempt
         * has failed due to a general provider issue.  This includes issues
         * around Bug_38087045.
         */
        public static final int FAILURE_ON_PROVIDER_ERROR = 18726;
        public static final int FAILURE_ON_LOGIN_ERROR = 1017;

        public static final Set<Integer> ORA_FAILURECODES_ON_CONNECTION =
                Set.of(FAILURE_ON_PROVIDER_ERROR, FAILURE_ON_LOGIN_ERROR);
        /**
         * The URL to poke when the OCI_INTERACTIVE mode has failed due to
         * a bind exception where-in the expect port for token callback is already
         * bound or where the user cancels or lets the connection auth expire and
         * the provider blocks forever waiting for a call that will never come.
         * Calling a GET on this url can work around the problem by simulating an
         * expected call on the token web server and awaking the blocked thread
         * into a failure code.
         */
        public static final String OCI_INTERACTIVE_WEB_SERVER_POKE_URL = "http://localhost:8181/token?";
        /**
         * The class name of the class to call clearAllCaches on (see below) when
         * the provider cache is suspected of being stuck in a permanent failure
         * state.
         */
        public static final String ORACLE_JDBC_PROVIDER_CACHE_CACHE_CONTROLLER_CLASSNAME = "oracle.jdbc.provider.cache.CacheController";
        /**
         * Certain calls that are serviced by certain providers can fail in the provider code
         * and this failure gets stuck in the caching mechanism of the provider framework.
         * When this happens, the provider cache will continue to throw the same error
         * regardless of whether the original failure condition has been cleared.  At this
         * point, at least until there is a fix, the only solution is to call
         * CacheController.clearAllCaches() in the classloader of the failing Driver.
         * This is the name of that method.
         */
        public static final String CLEAR_ALL_CACHES_METHOD_NAME = "clearAllCaches";
    }

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
                DATA_CHANGE_NOTIFICATION,
                JAVA_VIRTUAL_MACHINE
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
    public void initConnectorAuthentication(ConnectorProperties properties, @Nullable AuthenticationInfo authenticationInfo) {
        if (authenticationInfo == null) return;

        AuthenticationType authenticationType = authenticationInfo.getType();
        if (authenticationType == AuthenticationType.TOKEN) {
            AuthenticationTokenType tokenType = authenticationInfo.getTokenType();
            if (tokenType == OCI_INTERACTIVE) {
                properties.add(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_OCI_INTERACTIVE);
                copyCommonOciTokenProperties(properties,authenticationInfo);
            } else if (tokenType == OCI_API_KEY) {
                properties.add(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_OCI_API_KEY);
                properties.add(Property.ORACLE_JDBC_OCI_CONFIG_FILE, nvl(authenticationInfo.getTokenConfigFile(), ""));
                properties.add(Property.ORACLE_JDBC_OCI_PROFILE, nvl(authenticationInfo.getTokenProfile(), ""));
                copyCommonOciTokenProperties(properties,authenticationInfo);
            } else if (tokenType == AZURE_INTERACTIVE) {
                properties.add(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_AZURE_INTERACTIVE);
                properties.add(Property.ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI, authenticationInfo.getAzureDatabaseApplicationIdUri());
            } else if (tokenType == AZURE_SERVICE_PRINCIPAL_CERTIFICATE) {
                copyCommonAzureServicePrincipalProperties(properties, authenticationInfo);
                properties.add(Property.ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_FILE, authenticationInfo.getAzureClientCertificateFile());
                properties.add(Property.ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_PASSWORD,
                        Chars.toStringAcceptEmpty(authenticationInfo.getAzureClientCertificatePassword()));
            } else if (tokenType == AZURE_SERVICE_PRINCIPAL_TOKEN) {
                copyCommonAzureServicePrincipalProperties(properties, authenticationInfo);
                properties.add(Property.ORACLE_JDBC_AZURE_CLIENT_SECRET, Chars.toStringAcceptEmpty(authenticationInfo.getAzureClientSecret()));
            } else {
                //TODO...
            }
        } else {
            super.initConnectorAuthentication(properties, authenticationInfo);
        }
    }

    @Override
    public void initConnectorSession(ConnectorProperties properties, ConnectionSettings settings, SessionId sessionId) {
        super.initConnectorSession(properties, settings, sessionId);

        ConnectionType connectionType = sessionId.getConnectionType();
        String appName = "DB Navigator - " + connectionType.getName();
        properties.add(Property.SESSION_PROGRAM, appName);
    }

    @Override
    public void initConnectorDebugger(ConnectorProperties properties, ConnectionSettings settings) {
        Map<String, String> configProperties = settings.getPropertiesSettings().getProperties();

        // i check if we have got jdwpHostPort if yes i get a connection using CONNECTION_PROPERTY_THIN_DEBUG_JDWP property
        // TODO jdwpHostPort may remain resident if this stage is not reached for any reason... (maybe add transient properties container to settings)
        String jdwpHostPort = configProperties.remove("jdwpHostPort");
        if (Strings.isNotEmpty(jdwpHostPort)) {
            properties.add(Property.ORACLE_JDBC_DEBUG_JDWP, jdwpHostPort);
        }
    }

    private static void copyCommonOciTokenProperties(ConnectorProperties properties, AuthenticationInfo authenticationInfo) {
        // make sure to leave these null if the user didn't set them as the provider won't check for empty string
        Strings.ifNotEmpty(authenticationInfo.getAutonomousDatabaseCompartmentOcid(),
                compartmentOcid -> properties.add(Property.ORACLE_JDBC_OCI_COMPARTMENT, compartmentOcid));
        Strings.ifNotEmpty(authenticationInfo.getAutonomousDatabaseOcid(),
                databaseOcid -> properties.add(Property.ORACLE_JDBC_OCI_DATABASE, databaseOcid));
    }

    private static void copyCommonAzureServicePrincipalProperties(ConnectorProperties properties, AuthenticationInfo authenticationInfo) {
        properties.add(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_AZURE_SERVICE_PRINCIPAL);
        properties.add(Property.ORACLE_JDBC_SSL_SERVER_DN_MATCH, "yes");
        properties.add(Property.ORACLE_JDBC_AZURE_CLIENT_ID, authenticationInfo.getAzureClientId());
        properties.add(Property.ORACLE_JDBC_AZURE_TENANT_ID, authenticationInfo.getAzureTenantId());
        properties.add(Property.ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI, authenticationInfo.getAzureDatabaseApplicationIdUri());
    }

    @Override
    public boolean handleConnectionException(final ConnectionExceptionInfo info) {
        ConnectionExceptionVisitor visitor = new ConnectionExceptionVisitor();
        info.accept(visitor);
        // if a bind exception was thrown or the error was due to n provider failure code
        if (visitor.hasBindException() ||
                visitor.containsOraErrorCodes(ProviderErrorHandlingConstants.ORA_FAILURECODES_ON_CONNECTION)) {
            if (info.getAuthenticationInfo().getType() == AuthenticationType.TOKEN) {
                //all token provider connection problems require a workaround for matching
                //failures
                Background.run(() -> resetTokenProviderConnection(info));
            }
        }
        return false;
    }

    private static void resetTokenProviderConnection(ConnectionExceptionInfo info) {
        // TODO: use a backoff and retry?
        // unfreeze the busy socket (e.g. when auth browser is left unattended or closed without completing the authentication)
        if (info.getAuthenticationInfo().getTokenType() == OCI_INTERACTIVE &&
                !Sockets.tryToBindPort(ProviderErrorHandlingConstants.OCI_INTERACTIVE_TOKEN_RESPONSE_HTTP_PORT)) {
            /**
             *  @see ProviderErrorHandlingConstants.OCI_INTERACTIVE_WEB_SERVER_POKE_URL
             */
            Sockets.pokeWebServer(
                    ProviderErrorHandlingConstants.OCI_INTERACTIVE_WEB_SERVER_POKE_URL);
        }

        // clear the provider caches holding unsuccessful authentication state
        try {
            /**
             * @see ProviderErrorHandlingConstants.CLEAR_ALL_CACHES_METHOD_NAME
             */
            ClassLoader classLoader = info.getClassLoader();
            if (classLoader != null) {
                Class<?> cacheControllerClass =
                        Class.forName(
                                ProviderErrorHandlingConstants.ORACLE_JDBC_PROVIDER_CACHE_CACHE_CONTROLLER_CLASSNAME, true, classLoader);
                Method clearAllCaches = cacheControllerClass.getMethod(ProviderErrorHandlingConstants.CLEAR_ALL_CACHES_METHOD_NAME);
                clearAllCaches.invoke(null, new Object[0]);
            }
            else {
                Diagnostics.conditionallyLog(new NullPointerException("classLoader was null"));
            }
        } catch (final Exception e) {
            Diagnostics.conditionallyLog(e);
        }
    }
}