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

package com.dbn.mcp.deploy;

import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNCallableStatement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Creates the Graal application through the same database connection the MCP server was built
 * from. The application name and container image OCID are always bound as parameters - never
 * concatenated into the PL/SQL text.
 */
@Slf4j
@RequiredArgsConstructor
final class McpGraalApplicationManager {

    /**
     * POC resource shape, per the documented Graal demo: a long-running HTTP SERVER application
     * on the transaction-processing service, with the platform supplying DATABASE_URL and
     * OCI_GRAAL_DB_TOKEN itself (hence the empty environment).
     */
    /**
     * The environment is left empty on purpose: the platform injects DATABASE_URL and
     * OCI_GRAAL_DB_TOKEN itself, and the server's bind address is generated correctly in
     * application.yml rather than being overridden here.
     */
    private static final @NonNls String CREATE_APPLICATION_STATEMENT =
            "DECLARE\n" +
            "    l_application graalos.application_t;\n" +
            "BEGIN\n" +
            "    l_application := graalos.graalos_api_pkg.create_application(\n" +
            "        name                  => ?,\n" +
            "        container_image_id    => ?,\n" +
            "        type                  => 'SERVER',\n" +
            "        db_service_selector   => 'tp',\n" +
            "        ecpus                 => 1,\n" +
            "        memory_in_mbs         => 256,\n" +
            "        tmpfs_max_size_in_mbs => 10,\n" +
            "        max_concurrency       => NULL,\n" +
            "        environment           => NULL\n" +
            "    ).item;\n" +
            "END;";

    private final ConnectionRef connection;

    void createApplication(@NotNull McpGraalDeploymentInput input) throws SQLException {
        ConnectionHandler connectionHandler = ConnectionRef.ensure(connection);
        ConnectionContext context = new ConnectionContext(
                connectionHandler.getProject(), connectionHandler.getConnectionId(), null);

        PooledConnection.run(context, conn -> {
            DBNCallableStatement statement = null;
            try {
                statement = conn.prepareCall(CREATE_APPLICATION_STATEMENT);
                statement.setString(1, input.getApplicationName());
                statement.setString(2, input.getContainerImageOcid());
                statement.execute();
            } finally {
                Resources.close(statement);
            }
        });
    }
}
