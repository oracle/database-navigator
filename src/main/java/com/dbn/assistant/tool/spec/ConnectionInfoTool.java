/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.spec;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.ConnectionInfoToolImpl;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.Map;

import static com.dbn.assistant.tool.AssistantToolCategory.CONFIG_INFO_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolType.CONNECTION_INFO;

@ToolSpec(
        category = CONFIG_INFO_PROVIDER,
        type = CONNECTION_INFO,
        name = "Connection information",
        description =
                "Information about the database connection (such as database host, port, name, jdbc driver, authentication type, etc.). " +
                "Client secrets such as passwords or tokens are never exposed, but shown as redacted placeholders to indicate they are required.")
public interface ConnectionInfoTool extends AssistantTool {

    @FactorySpec(
            spec = ConnectionInfoTool.class,
            impl = ConnectionInfoToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ConnectionInfoTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LOAD_CONNECTION_INFORMATION")
    @UtilitySpec(
            name = "Load connection information",
            description = "Loads database connection and authentication information")
    ConnectionInformation loadConnectionInformation();

    @Data
    @Description("Database connection information")
    class ConnectionInformation{
        @Description("Config type (SID, Service Name, TNS, Easy Connect, etc.)")
        private String configType;

        @Description("Database host")
        private String databaseHost;

        @Description("Database port")
        private String databasePort;

        @Description("Database name (SID / Service Name)")
        private String databaseName;

        @Description("Server type (DEFAULT, DEDICATED, SHARED, POOLED)")
        private String serverType;

        @Description("Connection protocol (TCP, TCPS)")
        private String protocol;

        @Description("TNS folder")
        private String tnsFolder;

        @Description("TNS profile")
        private String tnsProfile;

        @Description("Connection parameters (for Easy-Connect)")
        private Map<String, String> connectionParameters;

        @Description("Connection properties (JDBC properties)")
        private Map<String, String> connectionProperties;

        @Description("Authentication information")
        private AuthenticationInformation authenticationInfo;
    }

    @Data
    @Description("Database authentication information")
    class AuthenticationInformation {
        @Description("Authentication type (User/password, OS credentials, authentication token)")
        private String authenticationType;

        @Description("User name")
        private String userName;

        @Description("User password (redacted/undisclosed)")
        private String password;

        @Description("Token type (OCI API Key, OCI Interactive, Azure Service Principal, etc.)")
        private String tokenType;

        @Description("Token authentication information")
        private TokenAuthenticationInfo tokenAuthenticationInfo;
    }

    @Data
    @Description("OCI token authentication")
    class TokenAuthenticationInfo {
        @Description("OCI token config file")
        private String ociTokenConfigFile;

        @Description("OCI token profile name")
        private String ociTokenProfile;

        @Description("OCI compartment OCID")
        private String ociCompartmentOcid;

        @Description("OCI database OCID")
        private String ociDatabaseOcid;



        @Description("Azure client identifier")
        private String azureClientId;

        @Description("Azure tenant identifier")
        private String azureTenantId;

        @Description("Azure client secret (redacted/undisclosed)")
        private String azureClientSecret;

        @Description("Azure client certificate file")
        private String azureClientCertificateFile;

        @Description("Azure client certificate password (redacted/undisclosed)")
        private String azureClientCertificatePassword;

        @Description("Azure database application Id URI")
        private String azureDatabaseAppIdUri;
    }
}
