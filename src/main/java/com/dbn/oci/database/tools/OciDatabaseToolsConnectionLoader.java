/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.oci.database.tools;

import com.dbn.common.util.Classes;
import com.dbn.oci.config.OciAuthenticationConfig;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.databasetools.DatabaseToolsClient;
import com.oracle.bmc.databasetools.model.ConnectionType;
import com.oracle.bmc.databasetools.model.DatabaseToolsConnectionSummary;
import com.oracle.bmc.databasetools.model.LifecycleState;
import com.oracle.bmc.databasetools.requests.ListDatabaseToolsConnectionsRequest;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.identity.requests.GetTenancyRequest;
import com.oracle.bmc.identity.requests.ListCompartmentsRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OciDatabaseToolsConnectionLoader {
    public List<OciCompartmentInfo> loadCompartments(OciAuthenticationConfig authentication) throws IOException {
        return Classes.withClassLoader(this, () -> {
            AuthenticationDetailsProvider provider = createAuthenticationProvider(authentication);
            try (IdentityClient client = IdentityClient.builder().build(provider)) {
                String tenancyId = provider.getTenantId();
                List<OciCompartmentInfo> compartments = new ArrayList<>();
                compartments.add(new OciCompartmentInfo(
                        tenancyId,
                        null,
                        client.getTenancy(GetTenancyRequest.builder().tenancyId(tenancyId).build()).getTenancy().getName()));

                ListCompartmentsRequest request = ListCompartmentsRequest.builder()
                        .compartmentId(tenancyId)
                        .compartmentIdInSubtree(true)
                        .accessLevel(ListCompartmentsRequest.AccessLevel.Accessible)
                        .build();
                for (Compartment compartment : client.getPaginators().listCompartmentsRecordIterator(request)) {
                    compartments.add(new OciCompartmentInfo(
                            compartment.getId(),
                            compartment.getCompartmentId(),
                            compartment.getName()));
                }
                return compartments;
            }
        });
    }

    public List<OciDatabaseToolsConnectionInfo> loadConnections(
            OciAuthenticationConfig authentication,
            String compartmentId) throws IOException {
        return Classes.withClassLoader(this, () -> {
            AuthenticationDetailsProvider provider = createAuthenticationProvider(authentication);
            try (DatabaseToolsClient client = DatabaseToolsClient.builder().build(provider)) {
                ListDatabaseToolsConnectionsRequest request = ListDatabaseToolsConnectionsRequest.builder()
                        .compartmentId(compartmentId)
                        .type(ConnectionType.OracleDatabase)
                        .build();
                List<OciDatabaseToolsConnectionInfo> connections = new ArrayList<>();
                for (DatabaseToolsConnectionSummary connection : client.getPaginators().listDatabaseToolsConnectionsRecordIterator(request)) {
                    if (!isUsableConnection(connection)) continue;

                    connections.add(new OciDatabaseToolsConnectionInfo(
                            connection.getId(),
                            connection.getCompartmentId(),
                            connection.getDisplayName()));
                }
                return connections;
            }
        });
    }

    private static boolean isUsableConnection(DatabaseToolsConnectionSummary connection) {
        LifecycleState lifecycleState = connection.getLifecycleState();
        return lifecycleState == LifecycleState.Active || lifecycleState == LifecycleState.Updating;
    }

    private static AuthenticationDetailsProvider createAuthenticationProvider(OciAuthenticationConfig authentication) throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(
                authentication.getConfigFile(),
                authentication.getProfile());
    }
}
